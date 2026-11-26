import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.process.ExecOperations
import org.gradle.kotlin.dsl.support.serviceOf
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import kotlin.reflect.full.safeCast

val execOps = serviceOf<ExecOperations>()

// Example ./gradlew releaseAllSDKs -Ptype=local
// Example ./gradlew releaseAllSDKs -Ptype=sonatype
tasks.register("releaseAllSDKs") {
    doLast {
        project.findProperty("type")
            ?.run(String::class::safeCast)
            ?.run {
                println("Converting parameter to an supported ReleaseType value")
                ReleaseType.valueOf(this.uppercase(Locale.getDefault()))
            }?.let { releaseType ->
                generateListOfModuleTasks(releaseType).forEach { task ->
                    println("Executing Task: $task")
                    execOps.exec {
                        val gradleCommand = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                            "gradlew.bat"
                        } else {
                            "./gradlew"
                        }
                        commandLine(gradleCommand, task.path)
                    }
                }
            } ?: throw Exception("Missing Type parameter")
    }
}

fun generateListOfModuleTasks(type: ReleaseType): List<Task> =
    compileListOfSDKs()
        .let { sdkModules ->
            // Maven Central rejects re-publishing an existing version, which fails the whole
            // Central Portal upload. Skip modules whose current version is already published
            // (e.g. pay/pos when only the BOM family is bumped).
            if (type == ReleaseType.SONATYPE) sdkModules.filterNot { it.isAlreadyPublished() } else sdkModules
        }
        .extractListOfPublishingTasks(type)

/**
 * SDK module configuration
 * @property parentModule The root module name
 * @property childModule The child module name (null for root-level modules like foundation)
 * @property env The environment type: "jvm" or "android"
 * @property repository The target repository: "sonatype" (com.reown) or "walletconnect" (com.walletconnect)
 */
data class SdkModule(
    val parentModule: String,
    val childModule: String?,
    val env: String,
    val repository: String = "reown"
)

fun compileListOfSDKs(): List<SdkModule> = mutableListOf(
    SdkModule("foundation", null, "jvm"),
    SdkModule("core", "android", "android"),
    SdkModule("core", "modal", "android"),
    SdkModule("protocol", "sign", "android"),
    SdkModule("protocol", "notify", "android"),
    SdkModule("product", "pay", "android", "walletconnect"), // Published under com.walletconnect
    SdkModule("product", "walletkit", "android"),
    SdkModule("product", "appkit", "android"),
    SdkModule("product", "pos", "android", "walletconnect"), // Published under com.walletconnect
).apply {
    // The BOM has to be last artifact
    add(SdkModule("core", "bom", "jvm"))
}

// This extension function will determine which task to run based on the type passed
fun List<SdkModule>.extractListOfPublishingTasks(type: ReleaseType): List<Task> = map { sdkModule ->
    val repositorySuffix = when {
        type == ReleaseType.LOCAL -> "MavenLocal"
        sdkModule.repository == "walletconnect" -> "WalletconnectRepository"
        else -> "ReownRepository"
    }

    val task = when (sdkModule.env) {
        "jvm" -> "$publishJvmRoot$repositorySuffix"
        "android" -> "$publishAndroidRoot$repositorySuffix"
        else -> throw Exception("Unknown Env: ${sdkModule.env}")
    }

    sdkModule.project().tasks.getByName(task)
}

fun SdkModule.project(): Project = if (childModule != null) {
    subprojects.first { it.name == parentModule }.subprojects.first { it.name == childModule }
} else {
    subprojects.first { it.name == parentModule }
}

fun SdkModule.isAlreadyPublished(): Boolean {
    val module = project()
    val group = module.extra.properties[KEY_PUBLISH_GROUP]?.toString() ?: DEFAULT_PUBLISH_GROUP
    val artifactId = requireNotNull(module.extra.properties[KEY_PUBLISH_ARTIFACT_ID]).toString()
    val version = requireNotNull(module.extra.properties[KEY_PUBLISH_VERSION]).toString()

    val published = isOnMavenCentral(group, artifactId, version)
    if (published) {
        println("Skipping $group:$artifactId:$version: already published on Maven Central")
    }
    return published
}

// Returns true only when the POM is confirmed on Maven Central; any other outcome
// (not found, network error, malformed URL) keeps the module in the release.
fun isOnMavenCentral(group: String, artifactId: String, version: String): Boolean {
    val pomUrl = "https://repo1.maven.org/maven2/${group.replace('.', '/')}/$artifactId/$version/$artifactId-$version.pom"
    return try {
        val connection = URI(pomUrl).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.responseCode == HttpURLConnection.HTTP_OK
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        println("Could not check Maven Central for $group:$artifactId:$version (${e.message}), publishing it")
        false
    }
}

private val publishJvmRoot = "publishMavenJvmPublicationTo"
private val publishAndroidRoot = "publishReleasePublicationTo"

enum class ReleaseType {
    LOCAL, SONATYPE
}
