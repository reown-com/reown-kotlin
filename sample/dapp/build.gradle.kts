plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.parcelize.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("signing-config")
}

android {
    namespace = "com.reown.sample.dapp"
    compileSdk = COMPILE_SDK

    defaultConfig {
        applicationId = "com.reown.sample.dapp"
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

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.palette:palette:1.0.0")

    implementation("io.insert-koin:koin-androidx-compose:3.4.3")
    implementation("io.coil-kt:coil-compose:2.3.0")
    implementation("androidx.compose.material:material-icons-core:1.7.1")

    implementation(libs.qrCodeGenerator)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.lifecycle)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(libs.androidx.compose.navigation.testing)

    implementation(libs.bundles.accompanist)

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    debugImplementation(project(":core:android"))
    debugImplementation(project(":product:appkit"))

    internalImplementation(project(":core:android"))
    internalImplementation(project(":product:appkit"))

    releaseImplementation(platform("com.reown:android-bom:$BOM_VERSION"))
    releaseImplementation("com.reown:android-core")
    releaseImplementation("com.reown:appkit")
}
