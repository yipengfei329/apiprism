import org.gradle.api.initialization.resolve.RepositoriesMode

rootProject.name = "apiprism"

pluginManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        mavenLocal()
        maven("https://maven.aliyun.com/repository/public")
        mavenCentral()
    }
}

include(":libs:registration-protocol")
project(":libs:registration-protocol").projectDir = file("libs/registration-protocol")

include(":libs:api-model")
project(":libs:api-model").projectDir = file("libs/api-model")

include(":libs:openapi-parser")
project(":libs:openapi-parser").projectDir = file("libs/openapi-parser")

include(":adapters:java:starter")
project(":adapters:java:starter").projectDir = file("adapters/java/starter")

include(":apps:mcp-gateway-engine")
project(":apps:mcp-gateway-engine").projectDir = file("apps/mcp-gateway-engine")

include(":apps:center-server")
project(":apps:center-server").projectDir = file("apps/center-server")

include(":examples:java-demo-service")
project(":examples:java-demo-service").projectDir = file("examples/java-demo-service")

include(":examples:spring-boot4-demo")
project(":examples:spring-boot4-demo").projectDir = file("examples/spring-boot4-demo")
