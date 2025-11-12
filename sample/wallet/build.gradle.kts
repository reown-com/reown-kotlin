plugins {
    id(libs.plugins.android.application.get().pluginId)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    id("signing-config")
//    id("io.sentry.android.gradle") version "3.12.0"
    alias(libs.plugins.compose.compiler)
}

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

    packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libuniffi_yttrium_utils.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libuniffi_yttrium_utils.so")
        jniLibs.pickFirsts.add("lib/x86_64/libuniffi_yttrium_utils.so")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }

}

dependencies {
    implementation(project(":sample:common"))
    implementation("androidx.compose.material3:material3:1.0.0-alpha08")

    // local .m2 build
    //    implementation("com.github.reown-com:yttrium-utils:unspecified")
    implementation("com.github.reown-com:yttrium:kotlin-utils-0.9.107") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("net.java.dev.jna:jna:5.17.0@aar")


    implementation("org.web3j:core:4.9.4")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Converter for JSON parsing using Gson
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp logging interceptor (optional, for debugging)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    implementation(libs.bundles.androidxAppCompat)

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")

    implementation("androidx.activity:activity-compose:1.6.1")
    implementation("androidx.palette:palette:1.0.0")

    // Glide
    implementation("com.github.skydoves:landscapist-glide:2.1.0")
    implementation("io.coil-kt:coil-svg:2.4.0")

    // Accompanist
    implementation(libs.bundles.accompanist)

    // Compose
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

    implementation(libs.coil)

    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.1.0")
    implementation("androidx.lifecycle:lifecycle-process:2.5.1")
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.2.0")
    implementation("androidx.camera:camera-lifecycle:1.2.0")
    implementation("androidx.camera:camera-view:1.0.0-alpha31")

    // Zxing
    implementation("com.google.zxing:core:3.5.0")

    // MixPanel
    implementation("com.mixpanel.android:mixpanel-android:7.3.1")

    // WalletConnect
    debugImplementation(project(":core:android"))
    debugImplementation(project(":product:walletkit"))
    debugImplementation(project(":protocol:notify"))

    internalImplementation(project(":core:android"))
    internalImplementation(project(":product:walletkit"))
    internalImplementation(project(":protocol:notify"))

    releaseImplementation(platform("com.reown:android-bom:$BOM_VERSION"))
    releaseImplementation("com.reown:android-core")
    releaseImplementation("com.reown:walletkit")
    releaseImplementation("com.reown:notify")
}