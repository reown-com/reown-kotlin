plugins {
    id("com.android.library")
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.google.ksp)
    id("publish-module-android")
    id("jacoco-report")
}

project.apply {
    extra[KEY_PUBLISH_ARTIFACT_ID] = ANDROID_CORE
    extra[KEY_PUBLISH_VERSION] = CORE_VERSION
    extra[KEY_SDK_NAME] = "Android Core"
}

android {
    namespace = "com.reown.android"
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = MIN_SDK

        buildConfigField(type = "String", name = "SDK_VERSION", value = "\"${requireNotNull(extra.get(KEY_PUBLISH_VERSION))}\"")
        buildConfigField("String", "PROJECT_ID", "\"${System.getenv("WC_CLOUD_PROJECT_ID") ?: ""}\"")
        buildConfigField("Integer", "TEST_TIMEOUT_SECONDS", "${System.getenv("TEST_TIMEOUT_SECONDS") ?: 30}")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments += mutableMapOf("clearPackageData" to "true")

        File("${rootDir.path}/gradle/consumer-rules").listFiles()?.let { proguardFiles ->
            consumerProguardFiles(*proguardFiles)
        }

        ndk.abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "${rootDir.path}/gradle/proguard-rules/sdk-rules.pro")
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
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }

    sourceSets {
        getByName("test").resources.srcDirs("src/test/resources")
    }

    buildFeatures {
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }

        registerManagedDevices()
    }
}

sqldelight {
    databases {
        create("AndroidCoreDatabase") {
            packageName.set("com.reown.android.sdk.core")
            schemaOutputDirectory.set(file("src/main/sqldelight/databases"))
//            generateAsync.set(true) // TODO: Enable once all repository methods have been converted to suspend functions
            verifyMigrations.set(true)
        }
    }
}

dependencies {
    debugApi(project(":foundation"))
    releaseApi("com.reown:foundation:$FOUNDATION_VERSION")

    // Use specific yttrium version for CI builds, default version for local builds
    val yttriumVersion = if (System.getenv("CI") == "true") {
        System.getenv("YTTRIUM_CI_VERSION") ?: "0.0.19-ci"
    } else {
        "0.9.43"
    }
    api("com.github.reown-com:yttrium:$yttriumVersion") //unspecified
    implementation("net.java.dev.jna:jna:5.15.0@aar")

    api(libs.coroutines)
    implementation(libs.scarlet.android)
    implementation(libs.bundles.sqlDelight)
    //noinspection UseTomlInstead
    api("net.zetetic:sqlcipher-android:4.6.1@aar")
    implementation(libs.relinker)
    api(libs.androidx.security)
    api(libs.koin.android)
    api(libs.timber)
    ksp(libs.moshi.ksp)
    api(libs.web3jCrypto)
    api(libs.bundles.kethereum)
    api(libs.bundles.retrofit)
    api(libs.beagle.logOkhttp)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

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