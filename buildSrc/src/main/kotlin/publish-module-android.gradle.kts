import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.api.DefaultAndroidSourceDirectorySet

plugins {
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

tasks {
    register("javadocJar", Jar::class) {
        dependsOn(named("dokkaHtml"))
        archiveClassifier.set("javadoc")
        from("${layout.buildDirectory}/dokka/html")
    }

    register("sourcesJar", Jar::class) {
        archiveClassifier.set("sources")
        from(
            (project.extensions.getByType<BaseExtension>().sourceSets.getByName("main").kotlin.srcDirs("kotlin") as DefaultAndroidSourceDirectorySet).srcDirs,
            (project.extensions.getByType<BaseExtension>().sourceSets.getByName("release").kotlin.srcDirs("kotlin") as DefaultAndroidSourceDirectorySet).srcDirs
        )
    }
}

(project as ExtensionAware).extensions.configure<LibraryExtension>("android") {
    publishing.singleVariant("release")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                afterEvaluate { from(components["release"]) }
                artifact(tasks.getByName("javadocJar"))
                artifact(tasks.getByName("sourcesJar"))

                groupId = project.extra.properties[KEY_PUBLISH_GROUP]?.toString() ?: DEFAULT_PUBLISH_GROUP
                artifactId = requireNotNull(project.extra[KEY_PUBLISH_ARTIFACT_ID]).toString()
                version = requireNotNull(project.extra[KEY_PUBLISH_VERSION]).toString()

                pom {
                    name.set("Reown ${requireNotNull(extra.get(KEY_SDK_NAME))}")
                    description.set("${requireNotNull(extra.get(KEY_SDK_NAME))} SDK for Reown")
                    url.set("https://github.com/reown-com/reown-kotlin")
                    licenses {
                        license {
                            name.set("WALLETCONNECT COMMUNITY LICENSE")
                            url.set("https://github.com/reown-com/reown-kotlin/blob/master/LICENSE")
                        }
                        license {
                            name.set("SQLCipher Community Edition")
                            url.set("https://www.zetetic.net/sqlcipher/license/")
                        }
                    }

                    developers {
                        developer {
                            id.set("KotlinSDKTeam")
                            name.set("Reown Kotlin")
                            email.set("mobile@reown.com ")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/reown-com/reown-kotlin.git")
                        developerConnection.set("scm:git:ssh://github.com/reown-com/reown-kotlin.git")
                        url.set("https://github.com/reown-com/reown-kotlin")
                    }
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("SIGNING_KEY_ID"),
        System.getenv("SIGNING_KEY"),
        System.getenv("SIGNING_PASSWORD")
    )
    sign(publishing.publications)
}