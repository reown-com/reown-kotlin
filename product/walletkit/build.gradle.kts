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
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = jvmVersion.toString()
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.time.ExperimentalTime"
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libuniffi_yttrium_wcpay.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libuniffi_yttrium_wcpay.so")
        jniLibs.pickFirsts.add("lib/x86_64/libuniffi_yttrium_wcpay.so")
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation("net.java.dev.jna:jna:5.17.0@aar")

    debugImplementation(project(":core:android"))
    debugImplementation(project(":protocol:sign"))

    releaseImplementation("com.reown:android-core:$CORE_VERSION")
    releaseImplementation("com.reown:sign:$SIGN_VERSION")

    debugImplementation(project(":product:pay"))
    releaseImplementation("com.walletconnect.pay:$PAY_VERSION")

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