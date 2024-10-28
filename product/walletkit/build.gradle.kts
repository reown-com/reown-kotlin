import java.net.URL

plugins {
    id("com.android.library")
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.google.ksp)
    id("publish-module-android")
    id("jacoco-report")
}

project.apply {
    extra[KEY_PUBLISH_ARTIFACT_ID] = WALLETKIT
    extra[KEY_PUBLISH_VERSION] = WALLETKIT_VERSION
    extra[KEY_SDK_NAME] = "walletkit"
}

android {
    namespace = "com.reown.walletkit"
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = MIN_SDK

        aarMetadata {
            minCompileSdk = MIN_SDK
        }

        buildConfigField(type = "String", name = "SDK_VERSION", value = "\"${requireNotNull(extra.get(KEY_PUBLISH_VERSION))}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "${rootDir.path}/gradle/proguard-rules/sdk-rules.pro", "${projectDir}/web3wallet-rules.pro")
        }
    }
    lint {
        abortOnError = true
        ignoreWarnings = true
        warningsAsErrors = false
    }

    compileOptions {
        sourceCompatibility = jvmVersion
        targetCompatibility = jvmVersion
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("${file(layout.buildDirectory)}/yttrium/kotlin-bindings")
            jniLibs.srcDirs("${file(layout.buildDirectory)}/yttrium/libs")
        }
    }

    kotlinOptions {
        jvmTarget = jvmVersion.toString()
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.time.ExperimentalTime"
    }

    buildFeatures {
        buildConfig = true
    }
}

tasks.register("downloadArtifacts") {
    doLast {
        val tagName = YTTRIUM_VERSION
        val downloadUrl = "https://github.com/reown-com/yttrium/releases/download/$tagName/kotlin-artifacts.zip"
        val outputFile = file("${file(layout.buildDirectory)}/kotlin-artifacts.zip")

        // Download the kotlin-artifacts.zip from GitHub Releases
        URL(downloadUrl).openStream().use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Extract the kotlin-artifacts.zip to the build directory
        copy {
            from(zipTree(outputFile))
            into("${file(layout.buildDirectory)}")
        }

        // Delete the zip file after extraction
        if (outputFile.exists()) {
            outputFile.delete()
            println("Deleted $outputFile")
        } else {
            println("File $outputFile does not exist")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("downloadArtifacts")
}

dependencies {
    implementation("net.java.dev.jna:jna:5.12.0@aar") //todo: extract to toml

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    debugImplementation(project(":core:android"))
    debugImplementation(project(":protocol:sign"))

    releaseImplementation("com.reown:android-core:$CORE_VERSION")
    releaseImplementation("com.reown:sign:$SIGN_VERSION")
}