plugins {
    id("com.android.library")
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.google.ksp)
    id("publish-module-android")
    id("jacoco-report")
}

project.apply {
    extra[KEY_PUBLISH_ARTIFACT_ID] = POS
    extra[KEY_PUBLISH_VERSION] = POS_VERSION
    extra[KEY_PUBLISH_GROUP] = "com.walletconnect"
    extra[KEY_SDK_NAME] = "pos"
}

android {
    namespace = "com.walletconnect.pos"
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = 29

        aarMetadata {
            minCompileSdk = 29
        }

        buildConfigField(type = "String", name = "SDK_VERSION", value = "\"${requireNotNull(extra.get(KEY_PUBLISH_VERSION))}\"")
        buildConfigField(type = "String", name = "CORE_API_BASE_URL", value = "\"https://api.pay.walletconnect.com\"")
        buildConfigField(type = "String", name = "PULSE_BASE_URL", value = "\"https://pulse.walletconnect.org\"")
        buildConfigField(type = "String", name = "POS_PROJECT_ID", value = "\"${System.getenv("POS_PROJECT_ID") ?: ""}\"")
        buildConfigField(type = "String", name = "INGEST_BASE_URL", value = "\"https://ingest.walletconnect.org/\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.okhttp.bom))
    implementation(libs.bundles.okhttp)

    implementation(libs.bundles.retrofit)

    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.ksp)

    implementation(libs.coroutines)

    testImplementation(libs.jUnit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)

    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.coroutines.test)
    androidTestUtil(libs.androidx.testOrchestrator)
    androidTestImplementation(libs.bundles.androidxAndroidTest)
}