import com.android.build.api.dsl.Packaging
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

     packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libuniffi_yttrium.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libuniffi_yttrium.so")
        jniLibs.pickFirsts.add("lib/x86_64/libuniffi_yttrium.so")
    }

    kotlinOptions {
        jvmTarget = jvmVersion.toString()
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.time.ExperimentalTime"
    }

    buildFeatures {
        buildConfig = true
    }
}

// Dependencies configuration
// Uses different yttrium versions for CI vs local builds:
// - CI builds: Uses version specified by YTTRIUM_CI_VERSION env var (or 0.9.18-ci as fallback)
// - Local builds: Uses the standard version (0.9.18)
// This allows testing with specific yttrium versions in CI without affecting local development
dependencies {
    implementation("net.java.dev.jna:jna:5.15.0@aar")
    
    // Use specific yttrium version for CI builds, default version for local builds
    val yttriumVersion = if (System.getenv("CI") == "true") {
        System.getenv("YTTRIUM_CI_VERSION") ?: "0.9.18-ci"
    } else {
        "0.9.18"
    }
    implementation("com.github.reown-com:yttrium:$yttriumVersion") //unspecified

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    debugImplementation(project(":core:android"))
    debugImplementation(project(":protocol:sign"))

    releaseImplementation("com.reown:android-core:$CORE_VERSION")
    releaseImplementation("com.reown:sign:$SIGN_VERSION")

    testImplementation(libs.bundles.androidxTest)
    testImplementation(libs.robolectric)
    testImplementation(libs.json)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.bundles.scarlet.test)
    testImplementation(libs.bundles.sqlDelight.test)
    testImplementation(libs.koin.test)

    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.core)

    androidTestUtil(libs.androidx.testOrchestrator)
    androidTestImplementation(libs.bundles.androidxAndroidTest)
}