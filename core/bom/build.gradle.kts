plugins {
    `java-platform`
    id("publish-module-java")
}

project.apply {
    extra[KEY_PUBLISH_ARTIFACT_ID] = ANDROID_BOM
    extra[KEY_PUBLISH_VERSION] = BOM_VERSION
    extra[KEY_SDK_NAME] = "Android BOM"
}

dependencies {
    constraints {
        api(project(":foundation"))
        api(project(":core:android"))
        api(project(":core:modal"))
        api(project(":protocol:sign"))
        api(project(":protocol:notify"))
        api(project(":product:appkit"))
        api(project(":product:walletkit"))
        api(project(":product:pos"))
    }
}