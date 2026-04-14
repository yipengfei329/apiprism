description = "OpenAPI document parser that produces the internal API model."

dependencies {
    api(project(":libs:api-model"))
    implementation(libs.swagger.parser)
    implementation(libs.slugify)
    implementation(libs.icu4j)
}
