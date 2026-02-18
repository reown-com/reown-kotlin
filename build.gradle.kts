import com.android.build.gradle.BaseExtension
import groovy.json.JsonSlurper
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpDelete
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Base64

plugins {
    alias(libs.plugins.nexusPublish)
    id("release-scripts")
    id("version-bump")
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvmVersion.toString()))
        }
    }

    configurations.configureEach {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "androidx.navigation" && requested.name == "navigation-compose") {
                    useVersion(libs.versions.androidxNavigation.get())
                }
                if (requested.group == "org.bouncycastle" && requested.name == "bcprov-jdk15on") {
                    useTarget(libs.bouncyCastle)
                }
            }
        }
    }
}

subprojects {
    afterEvaluate {
        if (hasProperty("android")) {
            extensions.configure(BaseExtension::class.java) {
                packagingOptions {
                    with(resources.excludes) {
                        add("META-INF/INDEX.LIST")
                        add("META-INF/DEPENDENCIES")
                        add("META-INF/LICENSE.md")
                        add("META-INF/NOTICE.md")
                    }
                }

                dependencies {
                    add("testImplementation", libs.mockk)
                    add("testImplementation", libs.jUnit)
                    add("testRuntimeOnly", libs.jUnit.engine)
                }
            }
        }

        plugins.withId(rootProject.libs.plugins.javaLibrary.get().pluginId) {
            dependencies {
                add("testImplementation", libs.mockk)
                add("testImplementation", libs.jUnit)
                add("testRuntimeOnly", libs.jUnit.engine)
            }
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

task<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

nexusPublishing {
    repositories {
//        project.version = "-SNAPSHOT"
        // Repository for com.reown artifacts (most SDKs)
        create("reown") {
            stagingProfileId.set(System.getenv("REOWN_SONATYPE_STAGING_PROFILE_ID"))
            packageGroup.set("com.reown")
            username.set(System.getenv("CENTRAL_PORTAL_USERNAME"))
            password.set(System.getenv("CENTRAL_PORTAL_PASSWORD"))
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
        // Repository for com.walletconnect artifacts (e.g., pay SDK)
        create("walletconnect") {
            stagingProfileId.set(System.getenv("WC_SONATYPE_STAGING_PROFILE_ID"))
            packageGroup.set("com.walletconnect")
            username.set(System.getenv("CENTRAL_PORTAL_USERNAME"))
            password.set(System.getenv("CENTRAL_PORTAL_PASSWORD"))
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
        }
    }
}

val nexusUsername: String get() = System.getenv("CENTRAL_PORTAL_USERNAME")
val nexusPassword: String get() = System.getenv("CENTRAL_PORTAL_PASSWORD")
val stagingApiUrl = "https://ossrh-staging-api.central.sonatype.com"
val manualApiUrl = "https://ossrh-staging-api.central.sonatype.com/manual"

tasks.register("closeAndReleaseMultipleRepositories") {
    group = "release"
    description = "Upload staging repositories to Central Portal and release"

    doLast {
        println("Starting closeAndReleaseMultipleRepositories task...")

        println("Fetching staging repositories...")
        val repos = fetchStagingRepositories()
        if (repos.isEmpty()) {
            throw RuntimeException("No staging repositories found. Publishing to staging may have failed.")
        }

        println("Found ${repos.size} staging repositories")
        repos.forEach { repo ->
            println("Repository: ${repo.key}, State: ${repo.state}")
        }

        val openRepos = repos.filter { it.state == "open" }
        val closedRepos = repos.filter { it.state == "closed" }

        println("Processing ${openRepos.size} open repositories and ${closedRepos.size} closed repositories")

        if (openRepos.isNotEmpty()) {
            println("Uploading ${openRepos.size} open repositories to Central Portal")
            uploadRepositoriesToPortal(openRepos)
        }

        if (closedRepos.isNotEmpty()) {
            println("Uploading ${closedRepos.size} closed repositories to Central Portal")
            uploadRepositoriesToPortal(closedRepos)
        }

        if (openRepos.isEmpty() && closedRepos.isEmpty()) {
            println("No repositories to upload to Portal")
            return@doLast
        }

        println("closeAndReleaseMultipleRepositories task completed successfully! Publish manually from the Maven Central Portal dashboard.")
    }
}

tasks.register("dropStagingRepositories") {
    group = "release"
    description = "Drop all staging repositories to clean up problematic ones"

    doLast {
        println("Starting dropStagingRepositories task...")

        println("Fetching staging repositories...")
        val repos = fetchStagingRepositories()
        if (repos.isEmpty()) {
            println("No staging repositories found to drop")
            return@doLast
        }

        println("Found ${repos.size} staging repositories")
        repos.forEach { repo ->
            println("Repository: ${repo.key}, State: ${repo.state}")
        }

        // Drop all repositories regardless of state
        println("Dropping all ${repos.size} staging repositories...")
        repos.forEachIndexed { index, repo ->
            println("Dropping repository ${index + 1}/${repos.size}: ${repo.key}")
            dropStagingRepository(repo.key)
            println("Completed dropping repository ${index + 1}/${repos.size}: ${repo.key}")
        }

        println("All staging repositories have been dropped successfully!")
        println("You can now run the closeAndReleaseMultipleRepositories task again to create fresh staging repositories.")
    }
}

data class StagingRepository(val key: String, val state: String, val portalDeploymentId: String?)

fun createHttpClient(socketTimeoutMs: Int = 60000): CloseableHttpClient = HttpClients.custom()
    .setDefaultRequestConfig(
        RequestConfig.custom()
            .setConnectTimeout(30000)
            .setSocketTimeout(socketTimeoutMs)
            .build()
    )
    .build()

fun authHeader(): String = "Bearer " + Base64.getEncoder().encodeToString("$nexusUsername:$nexusPassword".toByteArray())

fun fetchStagingRepositories(): List<StagingRepository> {
    val client = createHttpClient()
    try {
        val httpGet = HttpGet("$manualApiUrl/search/repositories").apply {
            setHeader("Authorization", authHeader())
        }

        val response = client.execute(httpGet)
        val responseBody = EntityUtils.toString(response.entity)
        if (response.statusLine.statusCode != 200) {
            throw RuntimeException("Failed: HTTP error code : ${response.statusLine.statusCode} $responseBody")
        }

        return parseRepositoriesResponse(responseBody)
    } finally {
        client.close()
    }
}

fun parseRepositoriesResponse(jsonResponse: String): List<StagingRepository> {
    val parsed = JsonSlurper().parseText(jsonResponse) as? Map<*, *> ?: return emptyList()
    val repos = parsed["repositories"] as? List<*> ?: return emptyList()
    return repos.mapNotNull { item ->
        val repo = item as? Map<*, *> ?: return@mapNotNull null
        val key = repo["key"] as? String ?: return@mapNotNull null
        val state = repo["state"] as? String ?: return@mapNotNull null
        val portalDeploymentId = repo["portal_deployment_id"] as? String
        StagingRepository(key, state, portalDeploymentId)
    }
}

fun uploadRepositoriesToPortal(repositories: List<StagingRepository>) {
    println("Starting upload of ${repositories.size} repositories to Central Portal...")
    val failedRepos = mutableListOf<String>()
    repositories.forEachIndexed { index, repo ->
        println("Uploading repository ${index + 1}/${repositories.size}: ${repo.key}")
        try {
            uploadRepositoryToPortal(repo.key)
            println("Completed upload of repository ${index + 1}/${repositories.size}: ${repo.key}")
        } catch (e: Exception) {
            println("Failed to upload repository ${index + 1}/${repositories.size}: ${repo.key} - ${e.message}")
            failedRepos.add(repo.key)
        }
    }
    if (failedRepos.isNotEmpty()) {
        throw RuntimeException("Failed to upload ${failedRepos.size}/${repositories.size} repositories: $failedRepos")
    }
    println("Completed upload of all ${repositories.size} repositories to Central Portal")
}

fun uploadRepositoryToPortal(repositoryKey: String, maxRetries: Int = 3) {
    val uploadUrl = "$manualApiUrl/upload/repository/$repositoryKey"
    println("Starting upload for repository: $repositoryKey")
    println("Upload URL: $uploadUrl")

    var lastException: Exception? = null
    var hadTimeout = false
    repeat(maxRetries) { attempt ->
        createHttpClient(socketTimeoutMs = 180_000).use { client ->
            try {
                val httpPost = HttpPost(uploadUrl).apply {
                    setHeader("Authorization", authHeader())
                    setHeader("Content-Type", "application/json")
                    entity = StringEntity("""{"publishing_type": "USER_MANAGED"}""")
                }

                println("Executing HTTP POST request (attempt ${attempt + 1}/$maxRetries)...")
                val response = client.execute(httpPost)
                val statusCode = response.statusLine.statusCode
                println("Received response with status: $statusCode")

                val responseBody = EntityUtils.toString(response.entity)
                println("Response body: $responseBody")

                if (statusCode == 200 || statusCode == 201) {
                    println("Successfully uploaded repository $repositoryKey to Central Portal")
                    return
                } else if (statusCode == 400 && hadTimeout) {
                    println("Got 400 after a previous timeout for $repositoryKey — the timed-out request likely succeeded. Treating as success.")
                    return
                } else {
                    lastException = RuntimeException("HTTP $statusCode - $responseBody")
                    println("Upload attempt ${attempt + 1}/$maxRetries failed for $repositoryKey: HTTP $statusCode")
                }
            } catch (e: java.net.SocketTimeoutException) {
                hadTimeout = true
                lastException = e
                println("Upload attempt ${attempt + 1}/$maxRetries timed out for $repositoryKey: ${e.message}")
            } catch (e: Exception) {
                lastException = e
                println("Upload attempt ${attempt + 1}/$maxRetries failed for $repositoryKey: ${e.message}")
            }
        }
        if (attempt < maxRetries - 1) {
            println("Retrying in 10 seconds...")
            Thread.sleep(10_000)
        }
    }
    throw RuntimeException("Failed to upload repository $repositoryKey after $maxRetries attempts", lastException)
}

fun dropStagingRepository(repositoryKey: String) {
    val client = createHttpClient()
    val dropUrl = "$manualApiUrl/drop/repository/$repositoryKey"

    println("Starting drop for repository: $repositoryKey")
    println("Drop URL: $dropUrl")

    val httpDelete = HttpDelete(dropUrl).apply {
        setHeader("Authorization", authHeader())
    }

    try {
        println("Executing HTTP DELETE request...")
        val response = client.execute(httpDelete)
        val statusCode = response.statusLine.statusCode
        println("Received response with status: $statusCode")

        val responseBody = if (response.entity != null) EntityUtils.toString(response.entity) else ""
        println("Response body length: ${responseBody.length}")

        if (statusCode == 200 || statusCode == 201 || statusCode == 204) {
            println("Successfully dropped repository $repositoryKey")
            if (responseBody.isNotEmpty()) {
                println("Response: $responseBody")
            }
        } else {
            throw RuntimeException("Failed to drop repository $repositoryKey: HTTP $statusCode - $responseBody")
        }
    } catch (e: Exception) {
        if (e is RuntimeException && e.message?.startsWith("Failed to drop") == true) throw e
        println("Exception during drop of repository $repositoryKey: ${e.message}")
        throw RuntimeException("Failed to drop repository $repositoryKey: ${e.message}", e)
    } finally {
        client.close()
    }
}

