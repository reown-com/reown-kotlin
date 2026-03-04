import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.android.build.gradle.BaseExtension
import java.util.Properties

plugins {
    id("com.google.firebase.appdistribution")
}

private val Project.secrets: Properties
    get() {
        val extra = rootProject.extra
        if (!extra.has("wc.secrets")) {
            val secretsFile = rootProject.file("secrets.properties")
            if (!secretsFile.exists()) {
                val isCi = System.getenv("CI")?.equals("true", ignoreCase = true) == true
                if (isCi) {
                    error("Missing secrets.properties in CI. Ensure the SECRETS_PROPERTIES secret is configured.")
                }
                secretsFile.writeText(
                    listOf(
                        "WC_KEYSTORE_ALIAS=mock_alias",
                        "WC_KEYSTORE_ALIAS_DEBUG=mock_alias_debug",
                        "WC_FILENAME_DEBUG=mock_debug.keystore",
                        "WC_STORE_PASSWORD_DEBUG=mock",
                        "WC_KEY_PASSWORD_DEBUG=mock",
                        "WC_FILENAME_INTERNAL=mock_internal.keystore",
                        "WC_STORE_PASSWORD_INTERNAL=mock",
                        "WC_KEY_PASSWORD_INTERNAL=mock",
                        "WC_FILENAME_UPLOAD=mock_upload.keystore",
                        "WC_STORE_PASSWORD_UPLOAD=mock",
                        "WC_KEY_PASSWORD_UPLOAD=mock",
                    ).joinToString("\n")
                )
                logger.warn("Generated mock secrets.properties (not suitable for production use)")
            }
            extra["wc.secrets"] = Properties().apply { secretsFile.inputStream().use { load(it) } }
        }
        return extra["wc.secrets"] as Properties
    }

project.extensions.configure(BaseExtension::class.java) {
    signingConfigs {
        create("upload") {
            storeFile = File(rootDir, secrets.getProperty("WC_FILENAME_UPLOAD"))
            storePassword = secrets.getProperty("WC_STORE_PASSWORD_UPLOAD")
            keyAlias = secrets.getProperty("WC_KEYSTORE_ALIAS")
            keyPassword = secrets.getProperty("WC_KEY_PASSWORD_UPLOAD")
        }

        create("internal_release") {
            storeFile = File(rootDir, secrets.getProperty("WC_FILENAME_INTERNAL"))
            storePassword = secrets.getProperty("WC_STORE_PASSWORD_INTERNAL")
            keyAlias = secrets.getProperty("WC_KEYSTORE_ALIAS")
            keyPassword = secrets.getProperty("WC_KEY_PASSWORD_INTERNAL")
        }

        getByName("debug") {
            if (File(rootDir, secrets.getProperty("WC_FILENAME_DEBUG")).exists()) {
                storeFile = File(rootDir, secrets.getProperty("WC_FILENAME_DEBUG"))
                storePassword = secrets.getProperty("WC_STORE_PASSWORD_DEBUG")
                keyAlias = secrets.getProperty("WC_KEYSTORE_ALIAS_DEBUG")
                keyPassword = secrets.getProperty("WC_KEY_PASSWORD_DEBUG")
            }
        }
    }

    buildTypes {
        // Google Play Internal Track
        getByName("release") {
            isMinifyEnabled = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("upload")
            versionNameSuffix = System.getenv("GITHUB_RUN_NUMBER")?.let { ".$it" } ?: ""
            defaultConfig.versionCode = "$SAMPLE_VERSION_CODE${System.getenv("GITHUB_RUN_NUMBER") ?: ""}".toInt()
            firebaseAppDistribution {
                artifactType = "AAB"
                serviceCredentialsFile = File(rootDir, "credentials.json").path
                groups = "internal_testers"
            }
        }

        // Firebase App Distribution
        create("internal") {
            isMinifyEnabled = true
            isDebuggable = true
            applicationIdSuffix(".internal")
            matchingFallbacks += listOf("debug")
            signingConfig = signingConfigs.getByName("internal_release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            versionNameSuffix = "${System.getenv("GITHUB_RUN_NUMBER")?.let { ".$it" } ?: ""}-internal"
            defaultConfig.versionCode = "$SAMPLE_VERSION_CODE${System.getenv("GITHUB_RUN_NUMBER") ?: ""}".toInt()
            firebaseAppDistribution {
                artifactType = "APK"
                serviceCredentialsFile = File(rootDir, "credentials.json").path
                groups = "internal_testers"
            }
        }

        getByName("debug") {
            applicationIdSuffix(".debug")
            signingConfig = signingConfigs.getByName("debug")
            versionNameSuffix = "${System.getenv("GITHUB_RUN_NUMBER")?.let { ".$it" } ?: ""}-debug"
            defaultConfig.versionCode = "$SAMPLE_VERSION_CODE${System.getenv("GITHUB_RUN_NUMBER") ?: ""}".toInt()
            firebaseAppDistribution {
                artifactType = "APK"
                serviceCredentialsFile = File(rootDir, "credentials.json").path
                groups = "internal_testers"
            }
        }
    }
}

dependencies {
    add("implementation", "com.google.firebase:firebase-appdistribution:16.0.0-beta12")
}