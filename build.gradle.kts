import com.android.build.gradle.BaseExtension
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpDelete
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Base64

plugins {
    alias(libs.plugins.nexusPublish)
    alias(libs.plugins.sonarqube)
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

////todo: user sonar cloud after repos are public
//sonar {
//    properties {
//        properties(
//            mapOf(
//                "sonar.projectKey" to "WalletConnect_WalletConnectKotlinV2",
//                "sonar.organization" to "walletconnect",
//                "sonar.host.url" to "https://sonarcloud.io",
//                "sonar.gradle.skipCompile" to true,
//                "sonar.coverage.exclusions" to "sample/**,**/di/**,/buildSrc/**,**/gradle/**,**/test/**,**/androidTest/**,**/build.gradle.kts",
//            )
//        )
//    }
//}

subprojects {
    apply(plugin = rootProject.libs.plugins.sonarqube.get().pluginId)

////todo: user sonar cloud after repos are public
//    extensions.configure<SonarExtension> {
//        setAndroidVariant("debug")
//
//        isSkipProject = name == "bom"
//        properties {
//            properties(
//                mapOf(
//                    "sonar.gradle.skipCompile" to true,
//                    "sonar.sources" to "${projectDir}/src/main/kotlin",
//                    "sonar.java.binaries" to layout.buildDirectory,
//                    "sonar.coverage.jacoco.xmlReportPaths" to "${layout.buildDirectory}/reports/jacoco/xml/jacoco.xml"
//                )
//            )
//        }
//    }

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
        sonatype {
            stagingProfileId.set(System.getenv("REOWN_SONATYPE_STAGING_PROFILE_ID"))
            packageGroup.set("com.reown")
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
            println("No staging repositories found")
            return@doLast
        }

        println("Found ${repos.size} staging repositories")
        repos.forEach { repo ->
            println("Repository: ${repo.key}, State: ${repo.state}")
        }

        // Upload each repository individually using their specific keys
        // This works better with the OSSRH Staging API than the defaultRepository endpoint
        val openRepos = repos.filter { it.state == "open" }
        val closedRepos = repos.filter { it.state == "closed" }

        println("Processing ${openRepos.size} open repositories and ${closedRepos.size} closed repositories")

        if (openRepos.isNotEmpty()) {
            println("Uploading ${openRepos.size} open repositories to Central Portal using individual repository keys")
            uploadRepositoriesToPortal(openRepos)
        }

        if (closedRepos.isNotEmpty()) {
            println("Uploading ${closedRepos.size} closed repositories to Central Portal using individual repository keys")
            uploadRepositoriesToPortal(closedRepos)
        }

        if (openRepos.isEmpty() && closedRepos.isEmpty()) {
            println("No repositories to upload to Portal")
            return@doLast
        }

        println("Starting to wait for artifacts to be available on Maven Central...")
        // Wait for artifacts to be available on Maven Central since we're using automatic publishing
        waitForArtifactsToBeAvailable()
        println("closeAndReleaseMultipleRepositories task completed successfully!")
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

fun fetchStagingRepositories(): List<StagingRepository> {
    val client = HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(30000) // 30 seconds
                .setSocketTimeout(60000) // 60 seconds
                .build()
        )
        .build()
    val httpGet = HttpGet("$manualApiUrl/search/repositories").apply {
        setHeader("Authorization", "Bearer " + Base64.getEncoder().encodeToString("$nexusUsername:$nexusPassword".toByteArray()))
    }

    val response = client.execute(httpGet)
    val responseBody = EntityUtils.toString(response.entity)
    if (response.statusLine.statusCode != 200) {
        throw RuntimeException("Failed: HTTP error code : ${response.statusLine.statusCode} $responseBody")
    }

    return parseRepositoriesResponse(responseBody)
}

fun parseRepositoriesResponse(jsonResponse: String): List<StagingRepository> {
    // Simple JSON parsing - in a real implementation you might want to use a proper JSON library
    val repositories = mutableListOf<StagingRepository>()

    // Extract repositories array from JSON response
    val repositoriesStart = jsonResponse.indexOf("\"repositories\":")
    if (repositoriesStart == -1) return repositories

    val arrayStart = jsonResponse.indexOf("[", repositoriesStart)
    val arrayEnd = jsonResponse.lastIndexOf("]")
    if (arrayStart == -1 || arrayEnd == -1) return repositories

    val repositoriesArray = jsonResponse.substring(arrayStart + 1, arrayEnd)

    // Parse each repository object (simple regex-based parsing)
    val repoPattern = """"key":\s*"([^"]+)".*?"state":\s*"([^"]+)".*?(?:"portal_deployment_id":\s*"([^"]*)")?""".toRegex()
    repoPattern.findAll(repositoriesArray).forEach { match ->
        val key = match.groupValues[1]
        val state = match.groupValues[2]
        val portalDeploymentId = match.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }
        repositories.add(StagingRepository(key, state, portalDeploymentId))
    }

    return repositories
}

fun uploadRepositoriesToPortal(repositories: List<StagingRepository>) {
    println("Starting upload of ${repositories.size} repositories to Central Portal...")
    repositories.forEachIndexed { index, repo ->
        println("Uploading repository ${index + 1}/${repositories.size}: ${repo.key}")
        uploadRepositoryToPortal(repo.key)
        println("Completed upload of repository ${index + 1}/${repositories.size}: ${repo.key}")
    }
    println("Completed upload of all ${repositories.size} repositories to Central Portal")
}

fun uploadRepositoryToPortal(repositoryKey: String) {
    val client = HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(30000) // 30 seconds
                .setSocketTimeout(60000) // 60 seconds
                .build()
        )
        .build()
    val uploadUrl = "$manualApiUrl/upload/repository/$repositoryKey"

    println("Starting upload for repository: $repositoryKey")
    println("Upload URL: $uploadUrl")

    val httpPost = HttpPost(uploadUrl).apply {
        setHeader("Authorization", "Bearer " + Base64.getEncoder().encodeToString("$nexusUsername:$nexusPassword".toByteArray()))
        setHeader("Content-Type", "application/json")
        // Use automatic publishing type to automatically release if validation passes
        entity = StringEntity("""{"publishing_type": "automatic"}""")
    }

    try {
        println("Executing HTTP POST request...")
        val response = client.execute(httpPost)
        println("Received response with status: ${response.statusLine.statusCode}")

        val responseBody = EntityUtils.toString(response.entity)
        println("Response body length: ${responseBody.length}")

        if (response.statusLine.statusCode == 200 || response.statusLine.statusCode == 201) {
            println("Successfully uploaded repository $repositoryKey to Central Portal")
            println("Response: $responseBody")
        } else {
            throw RuntimeException("Failed to upload repository $repositoryKey: HTTP ${response.statusLine.statusCode} - $responseBody")
        }
    } catch (e: Exception) {
        println("Exception during upload of repository $repositoryKey: ${e.message}")
        e.printStackTrace()
        throw RuntimeException("Failed to upload repository $repositoryKey: ${e.message}", e)
    } finally {
        try {
            httpPost.releaseConnection()
        } catch (e: Exception) {
            println("Warning: Failed to release connection: ${e.message}")
        }
    }
}

fun dropStagingRepository(repositoryKey: String) {
    val client = HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(30000) // 30 seconds
                .setSocketTimeout(60000) // 60 seconds
                .build()
        )
        .build()
    val dropUrl = "$manualApiUrl/drop/repository/$repositoryKey"

    println("Starting drop for repository: $repositoryKey")
    println("Drop URL: $dropUrl")

    val httpDelete = HttpDelete(dropUrl).apply {
        setHeader("Authorization", "Bearer " + Base64.getEncoder().encodeToString("$nexusUsername:$nexusPassword".toByteArray()))
    }

    try {
        println("Executing HTTP DELETE request...")
        val response = client.execute(httpDelete)
        println("Received response with status: ${response.statusLine.statusCode}")

        val responseBody = if (response.entity != null) {
            EntityUtils.toString(response.entity)
        } else {
            ""
        }
        println("Response body length: ${responseBody.length}")

        if (response.statusLine.statusCode == 200 || response.statusLine.statusCode == 201 || response.statusLine.statusCode == 204) {
            println("Successfully dropped repository $repositoryKey")
            if (responseBody.isNotEmpty()) {
                println("Response: $responseBody")
            }
        } else {
            throw RuntimeException("Failed to drop repository $repositoryKey: HTTP ${response.statusLine.statusCode} - $responseBody")
        }
    } catch (e: Exception) {
        println("Exception during drop of repository $repositoryKey: ${e.message}")
        e.printStackTrace()
        throw RuntimeException("Failed to drop repository $repositoryKey: ${e.message}", e)
    } finally {
        try {
            httpDelete.releaseConnection()
        } catch (e: Exception) {
            println("Warning: Failed to release connection: ${e.message}")
        }
    }
}

fun waitForArtifactsToBeAvailable() {
    val repoIds: List<String> = repoIdWithVersion.map { it.first }
    val client = HttpClients.custom()
        .setDefaultRequestConfig(
            RequestConfig.custom()
                .setConnectTimeout(30000) // 30 seconds
                .setSocketTimeout(60000) // 60 seconds
                .build()
        )
        .build()
    val artifactUrls = repoIdWithVersion.map { (repoId, version) ->
        println("Checking: https://repo1.maven.org/maven2/com/reown/$repoId/$version/")
        "https://repo1.maven.org/maven2/com/reown/$repoId/$version/"
    }
    val maxRetries = 40
    var attempt = 0
    val availableRepos = mutableSetOf<String>()

    while (availableRepos.size < repoIds.size && attempt < maxRetries) {
        artifactUrls.forEachIndexed { index, artifactUrl ->
            if (!availableRepos.contains(repoIds[index])) {
                val httpGet = HttpGet(artifactUrl)
                try {
                    val response = client.execute(httpGet)
                    val statusCode = response.statusLine.statusCode
                    EntityUtils.consume(response.entity) // Ensure the response is fully consumed
                    if (statusCode == 200 || statusCode == 201) {
                        println("Artifact for repository ${repoIds[index]} is now available.")
                        availableRepos.add(repoIds[index])
                    } else {
                        println("Artifact for repository ${repoIds[index]} not yet available. Status code: $statusCode")
                    }
                } catch (e: Exception) {
                    println("Error checking artifact for repository ${repoIds[index]}: ${e.message}")
                } finally {
                    httpGet.releaseConnection() // Ensure the connection is released
                }
            }
        }
        if (availableRepos.size < repoIds.size) {
            println("Waiting for artifacts to be available... Attempt: ${attempt + 1}")
            attempt++
            Thread.sleep(45000) // Wait for 45 seconds before retrying
        }
    }

    if (availableRepos.size < repoIds.size) {
        throw RuntimeException("Artifacts were not available after ${maxRetries * 45} seconds.")
    } else {
        println("All artifacts are now available.")
    }
}

private val repoIdWithVersion = listOf(
    Pair(ANDROID_BOM, BOM_VERSION),
    Pair(FOUNDATION, FOUNDATION_VERSION),
    Pair(ANDROID_CORE, CORE_VERSION),
    Pair(SIGN, SIGN_VERSION),
    Pair(NOTIFY, NOTIFY_VERSION),
    Pair(WALLETKIT, WALLETKIT_VERSION),
    Pair(APPKIT, APPKIT_VERSION),
    Pair(MODAL_CORE, MODAL_CORE_VERSION)
)