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
    // Javadoc → OpenAPI：therapi 在编译期提取 Javadoc 注释，SpringDoc v2 内置集成在运行时读取
    implementation(libs.therapi.javadoc.runtime)
    annotationProcessor(libs.therapi.javadoc.scribe)
}
