import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

// 在根项目上下文中提前解析 catalog 引用，
// 因为 subprojects {} 块的接收者是子项目，其扩展中无 libs
val lombokDep             = libs.lombok
val guavaDep              = libs.guava
val junitBomDep           = libs.junit.bom
val junitJupiterDep       = libs.junit.jupiter
val junitLauncherDep      = libs.junit.platform.launcher

// 需要发布到 Maven Central 的模块
val publishedModules = setOf(
    ":libs:registration-protocol",
    ":adapters:java:starter"
)

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension>("java") {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    dependencies {
        "compileOnly"(lombokDep)
        "annotationProcessor"(lombokDep)
        "testCompileOnly"(lombokDep)
        "testAnnotationProcessor"(lombokDep)
        "implementation"(guavaDep)

        "testImplementation"(platform(junitBomDep))
        "testImplementation"(junitJupiterDep)
        "testRuntimeOnly"(junitLauncherDep)
    }

    // 为需要发布的模块统一配置 maven-publish + signing + POM 元数据
    if (path in publishedModules) {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        afterEvaluate {
            extensions.configure<PublishingExtension>("publishing") {
                publications.withType<MavenPublication>().configureEach {
                    pom {
                        name.set(project.name)
                        description.set(project.description)
                        url.set("https://github.com/yipengfei/apiprism")

                        licenses {
                            license {
                                name.set("The Apache License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            }
                        }
                        developers {
                            developer {
                                id.set("yipengfei")
                                name.set("Pengfei Yi")
                                email.set("yipengfei329@gmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/yipengfei/apiprism.git")
                            developerConnection.set("scm:git:ssh://github.com/yipengfei/apiprism.git")
                            url.set("https://github.com/yipengfei/apiprism")
                        }
                    }
                }
            }

            extensions.configure<SigningExtension>("signing") {
                sign(extensions.getByType<PublishingExtension>().publications)
            }
        }
    }
}
