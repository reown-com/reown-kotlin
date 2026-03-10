import org.apache.tools.ant.taskdefs.condition.Os
import java.util.Locale
import kotlin.reflect.full.safeCast

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
                    exec {
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

fun generateListOfModuleTasks(type: ReleaseType): List<Task> = compileListOfSDKs().extractListOfPublishingTasks(type)

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

    val module = if (sdkModule.childModule != null) {
        subprojects.first { it.name == sdkModule.parentModule }.subprojects.first { it.name == sdkModule.childModule }
    } else {
        subprojects.first { it.name == sdkModule.parentModule }
    }

    module.tasks.getByName(task)
}

private val publishJvmRoot = "publishMavenJvmPublicationTo"
private val publishAndroidRoot = "publishReleasePublicationTo"

enum class ReleaseType {
    LOCAL, SONATYPE
}