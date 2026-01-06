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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.github.reown-com:yttrium-wcpay:unspecified")
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.coroutines)

    implementation(libs.androidx.core)
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.material)
    testImplementation(libs.jUnit)
}