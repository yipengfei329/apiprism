pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/central/") }
        maven { url = uri("https://maven.aliyun.com/repository/spring/") }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "apiprism"

include(":libs:registration-protocol")
project(":libs:registration-protocol").projectDir = file("libs/registration-protocol")

include(":libs:api-model")
project(":libs:api-model").projectDir = file("libs/api-model")

include(":libs:openapi-parser")
project(":libs:openapi-parser").projectDir = file("libs/openapi-parser")

include(":adapters:java:starter")
project(":adapters:java:starter").projectDir = file("adapters/java/starter")

include(":apps:center-server")
project(":apps:center-server").projectDir = file("apps/center-server")

include(":examples:java-demo-service")
project(":examples:java-demo-service").projectDir = file("examples/java-demo-service")

