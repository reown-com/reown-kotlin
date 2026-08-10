plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("signing-config")
//    id("io.sentry.android.gradle") version "3.12.0"
    alias(libs.plugins.compose.compiler)
}

// True only for E2E/test builds (CI sets ENABLE_TEST_MODE=true).
val enableTestMode = System.getenv("ENABLE_TEST_MODE")?.trim()?.lowercase() == "true"

android {
    namespace = "com.reown.sample.wallet"
    compileSdk = COMPILE_SDK
    // hash of all sdk versions from Versions.kt

    defaultConfig {
        applicationId = "com.reown.sample.wallet"
        minSdk = MIN_SDK
        targetSdk = TARGET_SDK
        versionName = SAMPLE_VERSION_NAME
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "PROJECT_ID", "\"${System.getenv("WC_CLOUD_PROJECT_ID") ?: ""}\"")
        buildConfigField("String", "PIMLICO_API_KEY", "\"${System.getenv("PIMLICO_API_KEY") ?: ""}\"")
        buildConfigField("String", "BOM_VERSION", "\"${BOM_VERSION}\"")
        buildConfigField("String", "TEST_WALLET_PRIVATE_KEY", "\"${System.getenv("TEST_WALLET_PRIVATE_KEY") ?: ""}\"")
        buildConfigField("boolean", "ENABLE_TEST_MODE", "$enableTestMode")

        ndk.abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
    }

    buildTypes {
        getByName("release") {
            manifestPlaceholders["pathPrefix"] = "/wallet_release"
            buildConfigField("String", "WALLET_APP_LINK", "\"https://appkit-lab.reown.com/wallet_release\"")
        }

        getByName("internal") {
            manifestPlaceholders["pathPrefix"] = "/wallet_internal"
            buildConfigField("String", "WALLET_APP_LINK", "\"https://appkit-lab.reown.com/wallet_internal\"")

            // Disable R8 for E2E builds only: the optimized dex intermittently fails ART
            // verification on the CI emulator and crashes on launch. Firebase builds keep R8 on.
            if (enableTestMode) {
                isMinifyEnabled = false
            }
        }

        getByName("debug") {
            manifestPlaceholders["pathPrefix"] = "/wallet_debug"
            buildConfigField("String", "WALLET_APP_LINK", "\"https://appkit-lab.reown.com/wallet_debug\"")
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


    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libuniffi_yttrium_utils.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libuniffi_yttrium_utils.so")
        jniLibs.pickFirsts.add("lib/x86_64/libuniffi_yttrium_utils.so")
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

}

dependencies {
    implementation(project(":sample:common"))
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation(libs.androidx.splashscreen)

    // local .m2 build
    //    implementation("com.github.reown-com:yttrium-utils:unspecified")
    implementation("com.github.reown-com.yttrium:yttrium-utils:0.10.58") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    implementation("org.web3j:core:4.9.8-hotfix")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    // Converter for JSON parsing using Gson
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    // OkHttp logging interceptor (optional, for debugging)
    implementation("com.squareup.okhttp3:logging-interceptor:5.4.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    implementation(libs.bundles.androidxAppCompat)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation(libs.androidx.lifecycleRuntime)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.palette:palette:1.0.0")

    // Glide
    implementation("com.github.skydoves:landscapist-glide:2.1.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    // Accompanist
    implementation(libs.bundles.accompanist)
    implementation(libs.androidx.compose.material.navigation)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation("androidx.compose.material:material-icons-core")
    implementation(libs.androidx.compose.navigation)
    implementation(libs.androidx.compose.lifecycle)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit)
    androidTestImplementation(libs.androidx.compose.navigation.testing)

    implementation(libs.coil)

    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.1.1")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.6.1")
    implementation("androidx.camera:camera-lifecycle:1.6.1")
    implementation("androidx.camera:camera-view:1.6.1")

    // Zxing
    implementation("com.google.zxing:core:3.5.4")

    // MixPanel
    implementation("com.mixpanel.android:mixpanel-android:8.8.0")

    testImplementation(libs.jUnit)

    // WalletConnect
    debugImplementation(project(":core:android")) {
        exclude(group = "com.github.reown-com", module = "yttrium")
    }
    debugImplementation(project(":product:walletkit"))
    debugImplementation(project(":protocol:notify"))

    internalImplementation(project(":core:android")) {
        exclude(group = "com.github.reown-com", module = "yttrium")
    }
    internalImplementation(project(":product:walletkit"))
    internalImplementation(project(":protocol:notify"))

    releaseImplementation(platform("com.reown:android-bom:$BOM_VERSION"))
    releaseImplementation("com.reown:android-core")
    releaseImplementation("com.reown:walletkit")
    releaseImplementation("com.reown:notify")

    debugImplementation(project(":product:pay")) {
        exclude(group = "com.github.reown-com", module = "yttrium")
    }
    internalImplementation(project(":product:pay")) {
        exclude(group = "com.github.reown-com", module = "yttrium")
    }
    releaseImplementation("com.walletconnect:pay:$PAY_VERSION") {
        exclude(group = "com.github.reown-com", module = "yttrium")
    }
}
