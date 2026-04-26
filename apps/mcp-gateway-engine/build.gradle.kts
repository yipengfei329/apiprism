plugins {
    alias(libs.plugins.spring.dependency.management)
}

description = "MCP Gateway Engine: auto-exposes registered API services as MCP tools with HTTP forwarding."

dependencies {
    api(project(":libs:api-model"))

    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.web)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.mcp.core)
    implementation(libs.mcp.json.jackson2)
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    annotationProcessor(libs.spring.boot.config.processor)

    testImplementation(libs.spring.boot.starter.web)
    testImplementation(libs.spring.boot.starter.test)
}
