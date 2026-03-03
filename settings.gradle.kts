// Auto-generate mock google-services.json for sample apps if missing.
// Allows fresh clones to build debug variants without manual setup.
// In CI, real files are created before Gradle runs, so this is safely skipped.
fun generateMockGoogleServicesJson(sampleDir: File, packageName: String) {
    val googleServicesFile = File(sampleDir, "google-services.json")
    if (!googleServicesFile.exists()) {
        googleServicesFile.writeText("""
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "mock-project-id",
    "storage_bucket": "mock-project-id.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
        "android_client_info": { "package_name": "$packageName" }
      },
      "oauth_client": [],
      "api_key": [{ "current_key": "mock-api-key" }],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    },
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000001",
        "android_client_info": { "package_name": "$packageName.debug" }
      },
      "oauth_client": [],
      "api_key": [{ "current_key": "mock-api-key" }],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    },
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000002",
        "android_client_info": { "package_name": "$packageName.internal" }
      },
      "oauth_client": [],
      "api_key": [{ "current_key": "mock-api-key" }],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    }
  ],
  "configuration_version": "1"
}
        """.trimIndent())
        logger.warn("Generated mock google-services.json in ${sampleDir.name}/ (not suitable for production use)")
    }
}

mapOf(
    "wallet" to "com.reown.sample.wallet",
    "dapp" to "com.reown.sample.dapp",
    "modal" to "com.reown.sample.modal"
).forEach { (sampleName, packageName) ->
    val sampleDir = File(rootDir, "sample/$sampleName")
    if (sampleDir.exists()) {
        generateMockGoogleServicesJson(sampleDir, packageName)
    }
}

rootProject.name = "Reown Kotlin"

val excludedDirs = listOf(
    "undefined",
    ".idea",
    ".maestro",
    ".git",
    "build",
    ".gradle",
    ".github",
    "buildSrc",
    "gradle",
    "docs",
    "licenses",
    "walletconnectv2",
    ".kotlin",
    ".claude",
    ".context"
)
// TODO: Add to rootModules when new module is added to the project root directory
val rootModules = listOf("foundation")

File(rootDir.path).listFiles { file -> file.isDirectory && file.name !in excludedDirs }?.forEach { childDir ->
    if (childDir.name !in rootModules) {
        childDir.listFiles { dir -> dir.isDirectory && dir.name !in excludedDirs}?.forEach { moduleDir ->
            val module = ":${moduleDir.parentFile.name}:${moduleDir.name}"
            include(module)
            project(module).projectDir = moduleDir
        }
    } else {
        include(":${childDir.name}")
    }
}

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
        //todo: remove me - https://github.com/rustls/rustls-platform-verifier/issues/115
        maven(url = "/Users/jakub/.cargo/registry/src/index.crates.io-1949cf8c6b5b557f/rustls-platform-verifier-android-0.1.1/maven")
        jcenter() // Warning: this repository is going to shut down soon
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}