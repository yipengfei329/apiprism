plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

description = "Java backend for the APIPrism center."

dependencies {
    implementation(project(":libs:registration-protocol"))
    implementation(project(":libs:api-model"))
    implementation(project(":libs:openapi-parser"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation(libs.flyway.core)
    implementation(libs.hypersistence.tsid)
    runtimeOnly(libs.h2)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<Jar>("jar") {
    enabled = false
}
