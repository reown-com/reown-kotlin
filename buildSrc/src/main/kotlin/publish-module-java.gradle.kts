plugins {
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

tasks {
    plugins.withId("java") {
        register("javadocJar", Jar::class) {
            dependsOn(named("dokkaHtml"))
            archiveClassifier.set("javadoc")
            from("${layout.buildDirectory}/dokka/html")
        }
        register("sourceJar", Jar::class) {
            archiveClassifier.set("sources")
            from(((project as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer).getByName("main").allSource)
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("mavenJvm") {
                plugins.withId("java") {
                    from(components["java"])
                    artifact(tasks.getByName("sourceJar"))
                    artifact(tasks.getByName("javadocJar"))
                }

                plugins.withId("java-platform") {
                    from(components["javaPlatform"])
                }

                groupId = "com.reown"
                artifactId = requireNotNull(extra.get(KEY_PUBLISH_ARTIFACT_ID)).toString()
                version = requireNotNull(extra.get(KEY_PUBLISH_VERSION)).toString()

                pom {
                    name.set("Reown ${requireNotNull(extra.get(KEY_SDK_NAME))}")
                    description.set("${requireNotNull(extra.get(KEY_SDK_NAME))} SDK for Reown")
                    url.set("https://github.com/reown-con/reown-kotlin")

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
                        connection.set("scm:git:git://github.com/reown-con/reown-kotlin.git")
                        developerConnection.set("scm:git:ssh://github.com/reown-con/reown-kotlin.git")
                        url.set("https://github.com/reown-con/reown-kotlin")
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