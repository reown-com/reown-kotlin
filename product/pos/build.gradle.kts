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
    extra[KEY_SDK_NAME] = "pos"
}

android {
    namespace = "com.reown.pos"
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
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    debugImplementation(project(":core:android"))
    debugImplementation(project(":protocol:sign"))

    releaseImplementation("com.reown:android-core:$CORE_VERSION")
    releaseImplementation("com.reown:sign:$SIGN_VERSION")

    implementation(libs.androidx.core)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.material)

    testImplementation(libs.jUnit)

    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.core)

    androidTestUtil(libs.androidx.testOrchestrator)
    androidTestImplementation(libs.bundles.androidxAndroidTest)
}