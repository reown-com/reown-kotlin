# **AppKit - Kotlin**

# Installation

Kotlin implementation of AppKit for Android applications.

Android Core ![Maven Central](https://img.shields.io/maven-central/v/com.walletconnect/android-core)

AppKit ![Maven Central](https://img.shields.io/maven-central/v/com.walletconnect/web3modal)

## Requirements

* Android min SDK 23
* Java 11

## Installation
root/build.gradle.kts:
```gradle
allprojects {
   repositories {
      mavenCentral()
      maven { url "https://jitpack.io" }
   }
}
```

app/build.gradle.kts

```gradle
implementation(platform("com.reown:android-bom:$BOM_VERSION"))
implementation("com.reown:android-core")
implementation("com.reown:appkit")
```

