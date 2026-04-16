plugins {
    application
}

description = "Spring Boot 4.x 环境下的 APIPrism starter 集成验证 Demo。"

val springBoot4Version = "4.0.3"
val springdoc3Version = "3.0.3"

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:$springBoot4Version"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":adapters:java:starter"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdoc3Version")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.33")
}

application {
    mainClass.set("ai.apiprism.demo.boot4.Boot4DemoApplication")
}
