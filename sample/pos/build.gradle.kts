plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
    id("signing-config")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.reown.sample.pos"
    compileSdk = COMPILE_SDK

    defaultConfig {
        applicationId = "com.reown.sample.pos"
        minSdk = 29
        targetSdk = TARGET_SDK
        versionName = SAMPLE_VERSION_NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "PROJECT_ID", "\"${System.getenv("WC_CLOUD_PROJECT_ID") ?: ""}\"")
        buildConfigField("String", "BOM_VERSION", "\"${BOM_VERSION}\"")
        buildConfigField("String", "MERCHANT_API_KEY", "\"${System.getenv("MERCHANT_API_KEY") ?: ""}\"")
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

    debugImplementation(project(":product:pos"))
    internalImplementation(project(":product:pos"))
    releaseImplementation("com.walletconnect.pos:0.0.1")

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

    implementation(libs.androidx.core)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.material)

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    testImplementation(libs.jUnit)
}