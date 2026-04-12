FROM gradle:8.3.0-jdk21 AS builder
WORKDIR /workspace

# 第一层：仅复制构建脚本，依赖不变时可命中 Docker 层缓存
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/

# 逐模块复制 build.gradle.kts，源码变动不会使此层失效
COPY platform/contracts/build.gradle.kts            platform/contracts/
COPY platform/canonical-model/build.gradle.kts      platform/canonical-model/
COPY platform/normalization/build.gradle.kts        platform/normalization/
COPY adapters/java/core/build.gradle.kts            adapters/java/core/
COPY adapters/java/starter/build.gradle.kts         adapters/java/starter/
COPY apps/center-server/build.gradle.kts            apps/center-server/

# 预热依赖下载（构建脚本不变时此层可复用，加速后续 CI/CD 构建）
RUN gradle :apps:center-server:dependencies --no-daemon

# 第二层：复制源码并执行实际构建
COPY platform/ platform/
COPY adapters/ adapters/
COPY apps/center-server/ apps/center-server/

RUN gradle :apps:center-server:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /workspace/apps/center-server/build/libs/*.jar /app/center-server.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/center-server.jar"]
