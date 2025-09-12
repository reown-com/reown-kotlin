plugins {
    id(libs.plugins.android.library.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.google.ksp)
    id("publish-module-android")
    id("jacoco-report")
}

project.apply {
    extra[KEY_PUBLISH_ARTIFACT_ID] = SIGN
    extra[KEY_PUBLISH_VERSION] = SIGN_VERSION
    extra[KEY_SDK_NAME] = "Sign"
}

android {
    namespace = "com.reown.sign"
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = MIN_SDK

        aarMetadata {
            minCompileSdk = MIN_SDK
        }

        buildConfigField(
            type = "String",
            name = "SDK_VERSION",
            value = "\"${requireNotNull(extra.get(KEY_PUBLISH_VERSION))}\""
        )
        buildConfigField(
            "String",
            "PROJECT_ID",
            "\"${System.getenv("WC_CLOUD_PROJECT_ID") ?: ""}\""
        )
        buildConfigField(
            "Integer",
            "TEST_TIMEOUT_SECONDS",
            "${System.getenv("TEST_TIMEOUT_SECONDS") ?: 10}"
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments += mutableMapOf("clearPackageData" to "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "${rootDir.path}/gradle/proguard-rules/sdk-rules.pro"
            )
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

    kotlinOptions {
        jvmTarget = jvmVersion.toString()
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.time.ExperimentalTime"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        registerManagedDevices()
    }

    buildFeatures {
        buildConfig = true
    }
}

sqldelight {
    databases {
        create("SignDatabase") {
            packageName.set("com.reown.sign")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
//            generateAsync.set(true) // TODO: Enable once all repository methods have been converted to suspend functions
            verifyMigrations.set(true)
            verifyDefinitions.set(true)
        }
    }
}

dependencies {
    debugImplementation(project(":core:android"))
    releaseImplementation("com.reown:android-core:$CORE_VERSION")

    implementation("org.msgpack:msgpack-core:0.9.1")

    ksp(libs.moshi.ksp)
    implementation(libs.bundles.sqlDelight)

    testImplementation(libs.bundles.androidxTest)
    testImplementation(libs.robolectric)
    testImplementation(libs.json)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.bundles.scarlet.test)
    testImplementation(libs.bundles.sqlDelight.test)
    testImplementation(libs.koin.test)

    androidTestUtil(libs.androidx.testOrchestrator)
    androidTestImplementation(libs.bundles.androidxAndroidTest)
}