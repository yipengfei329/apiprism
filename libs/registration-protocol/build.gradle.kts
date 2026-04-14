plugins {
    `maven-publish`
}

description = "Adapter-to-center registration protocol types shared across all APIPrism components."

dependencies {
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "apiprism-registration-protocol"
        }
    }
}
