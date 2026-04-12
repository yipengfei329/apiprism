plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

description = "APIPrism 功能验证 Demo 服务，覆盖复杂参数场景。"

dependencies {
    implementation(project(":adapters:java:starter"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.springdoc.webmvc)
    // SpringDoc UI（Swagger UI）
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
}
