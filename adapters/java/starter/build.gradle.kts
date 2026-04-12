plugins {
    `maven-publish`
}

description = "Spring Boot starter for discovering and registering OpenAPI specs with APIPrism."

dependencies {
    api(project(":libs:registration-protocol"))

    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.jackson.databind)
    implementation(libs.spring.web)
    implementation(libs.spring.retry)
    implementation(libs.springdoc.webmvc)

    annotationProcessor(libs.spring.boot.config.processor)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.test)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "apiprism-spring-boot-starter"
        }
    }
}
