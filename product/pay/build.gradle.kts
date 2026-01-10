plugins {
    id("com.android.library")
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.google.ksp)
    id("publish-module-android")
}

project.apply {
    extra[KEY_PUBLISH_ARTIFACT_ID] = PAY
    extra[KEY_PUBLISH_VERSION] = PAY_VERSION
    extra[KEY_SDK_NAME] = "WalletConnectPay"
}

android {
    namespace = "com.walletconnect.pay"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 23

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField(type = "String", name = "SDK_VERSION", value = "\"${requireNotNull(extra.get(KEY_PUBLISH_VERSION))}\"")
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
    packaging {
        jniLibs.pickFirsts.add("lib/arm64-v8a/libuniffi_yttrium_wcpay.so")
        jniLibs.pickFirsts.add("lib/armeabi-v7a/libuniffi_yttrium_wcpay.so")
        jniLibs.pickFirsts.add("lib/x86_64/libuniffi_yttrium_wcpay.so")
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    //local
//    implementation("com.github.reown-com:yttrium-wcpay:unspecified")

    //jitpack
    implementation("com.github.reown-com.yttrium:yttrium-wcpay:0.9.119") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("net.java.dev.jna:jna:5.17.0@aar")

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coroutines)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.material)
    testImplementation(libs.jUnit)
}