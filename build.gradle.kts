import com.android.build.gradle.BaseExtension
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Base64
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.nexusPublish)
    alias(libs.plugins.sonarqube)
    id("release-scripts")
    id("version-bump")
}

allprojects {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = jvmVersion.toString()
        }
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "androidx.navigation" && requested.name == "navigation-compose") {
                useVersion(libs.versions.androidxNavigation.get())
            }
            if (requested.group == "org.bouncycastle" && requested.name == "bcprov-jdk15on") {
                useTarget(libs.bouncyCastle)
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
        val repos = fetchStagingRepositories()
        if (repos.isEmpty()) {
            println("No staging repositories found")
            return@doLast
        }
        
        println("Found ${repos.size} staging repositories")
        repos.forEach { repo ->
            println("Repository: ${repo.key}, State: ${repo.state}")
        }
        
        // Upload closed repositories to Central Portal
        val closedRepos = repos.filter { it.state == "closed" }
        if (closedRepos.isNotEmpty()) {
            uploadRepositoriesToPortal(closedRepos)
        } else {
            println("No closed repositories to upload to Portal")
        }
    }
}

data class StagingRepository(val key: String, val state: String, val portalDeploymentId: String?)

fun fetchStagingRepositories(): List<StagingRepository> {
    val client = HttpClients.createDefault()
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
    repositories.forEach { repo ->
        println("Uploading repository ${repo.key} to Central Portal...")
        uploadRepositoryToPortal(repo.key)
    }
}

fun uploadRepositoryToPortal(repositoryKey: String) {
    val client = HttpClients.createDefault()
    val uploadUrl = "$manualApiUrl/upload/repository/$repositoryKey"
    
    val httpPost = HttpPost(uploadUrl).apply {
        setHeader("Authorization", "Bearer " + Base64.getEncoder().encodeToString("$nexusUsername:$nexusPassword".toByteArray()))
        setHeader("Content-Type", "application/json")
        // Use manual publishing type to automatically release if validation passes
        entity = StringEntity("""{"publishing_type": "user_managed"}""")
    }

    val response = client.execute(httpPost)
    val responseBody = EntityUtils.toString(response.entity)
    
    if (response.statusLine.statusCode == 200 || response.statusLine.statusCode == 201) {
        println("Successfully uploaded repository $repositoryKey to Central Portal")
        println("Response: $responseBody")
    } else {
        throw RuntimeException("Failed to upload repository $repositoryKey: HTTP ${response.statusLine.statusCode} - $responseBody")
    }
}