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
    implementation(libs.swagger.core.jakarta)

    // SpringDoc 为可选依赖：用户按自身 Spring Boot 版本自行引入匹配的 springdoc 版本，
    // starter 在运行时自动检测并启用进程内 OpenAPI 获取；未引入时回退到 HTTP 方式。
    compileOnly(libs.springdoc.webmvc)

    annotationProcessor(libs.spring.boot.config.processor)
    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.springdoc.webmvc)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "apiprism-spring-boot-starter"
        }
    }
}
