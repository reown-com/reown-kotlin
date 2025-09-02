plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
//    alias(libs.plugins.google.services)
//    alias(libs.plugins.firebase.crashlytics)
    id("signing-config")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.reown.sample.pos"
    compileSdk = COMPILE_SDK

    defaultConfig {
        applicationId = "com.reown.sample.pos"
        minSdk = MIN_SDK
        targetSdk = TARGET_SDK
        versionName = SAMPLE_VERSION_NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "PROJECT_ID", "\"${System.getenv("WC_CLOUD_PROJECT_ID") ?: ""}\"")
        buildConfigField("String", "BOM_VERSION", "\"${BOM_VERSION}\"")
    }

    buildTypes {
        getByName("release") {
            manifestPlaceholders["pathPrefix"] = "/dapp_release"
            buildConfigField("String", "DAPP_APP_LINK", "\"https://appkit-lab.reown.com/dapp_release\"")
        }

        getByName("internal") {
            manifestPlaceholders["pathPrefix"] = "/dapp_internal"
            buildConfigField("String", "DAPP_APP_LINK", "\"https://appkit-lab.reown.com/dapp_internal\"")

        }

        getByName("debug") {
            manifestPlaceholders["pathPrefix"] = "/dapp_debug"
            buildConfigField("String", "DAPP_APP_LINK", "\"https://appkit-lab.reown.com/dapp_debug\"")
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
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(project(":sample:common"))

    debugImplementation(project(":core:android"))
    debugImplementation(project(":product:pos"))

    internalImplementation(project(":core:android"))
    internalImplementation(project(":product:pos"))

    releaseImplementation(platform("com.reown:android-bom:$BOM_VERSION"))
    releaseImplementation("com.reown:android-core")
    releaseImplementation("com.reown:pos")

    implementation(libs.bundles.accompanist)

    implementation(libs.qrCodeGenerator)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.lifecycle)

    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("io.coil-kt.coil3:coil-compose:3.3.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.3.0")
    implementation("com.google.zxing:core:3.5.3")

//    implementation(platform(libs.firebase.bom))
//    implementation(libs.bundles.firebase)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    testImplementation(libs.jUnit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}