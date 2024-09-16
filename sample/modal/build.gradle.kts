plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    id(libs.plugins.kotlin.kapt.get().pluginId)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("signing-config")
}

android {
    namespace = "com.reown.sample.modal"
    compileSdk = COMPILE_SDK

    defaultConfig {
        applicationId = "com.reown.sample.modal"
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
            manifestPlaceholders["pathPrefix"] = "/lab_release"
            buildConfigField("String", "LAB_APP_LINK", "\"https://dev.lab.web3modal.com/lab_release\"")
        }

        getByName("internal") {
            manifestPlaceholders["pathPrefix"] = "/lab_internal"
            buildConfigField("String", "LAB_APP_LINK", "\"https://dev.lab.web3modal.com/lab_internal\"")

        }

        getByName("debug") {
            manifestPlaceholders["pathPrefix"] = "/lab_debug"
            buildConfigField("String", "LAB_APP_LINK", "\"https://dev.lab.web3modal.com/lab_debug\"")
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
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    implementation(project(":sample:common"))
    implementation("androidx.compose.material:material-icons-core:1.7.1")

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

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
    implementation(libs.bundles.androidxAppCompat)
    implementation(libs.bundles.androidxLifecycle)
    api(libs.bundles.androidxNavigation)

    debugImplementation(project(":core:android"))
    debugImplementation(project(":product:appkit"))

    internalImplementation(project(":core:android"))
    internalImplementation(project(":product:appkit"))

    releaseImplementation(platform("com.reown:android-bom:$BOM_VERSION"))
    releaseImplementation("com.reown:android-core")
    releaseImplementation("com.reown:appkit")
}