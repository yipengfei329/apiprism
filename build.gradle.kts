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
}
