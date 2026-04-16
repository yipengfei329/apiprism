# syntax=docker/dockerfile:1.7

FROM gradle:9.4.1-jdk21 AS builder
WORKDIR /workspace

# 第一层：仅复制构建脚本，依赖不变时可命中 Docker 层缓存
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/

# 逐模块复制 build.gradle.kts，源码变动不会使此层失效
COPY libs/registration-protocol/build.gradle.kts    libs/registration-protocol/
COPY libs/api-model/build.gradle.kts                libs/api-model/
COPY libs/openapi-parser/build.gradle.kts           libs/openapi-parser/
COPY adapters/java/starter/build.gradle.kts         adapters/java/starter/
COPY examples/java-demo-service/build.gradle.kts    examples/java-demo-service/
COPY apps/center-server/build.gradle.kts            apps/center-server/

# 直接使用与仓库一致版本的官方 Gradle 镜像，避免容器内重复下载 wrapper 分发包
RUN --mount=type=cache,id=gradle-home,target=/home/gradle/.gradle \
    gradle :apps:center-server:dependencies --no-daemon

# 第二层：复制源码并执行实际构建
COPY libs/ libs/
COPY adapters/ adapters/
COPY examples/ examples/
COPY apps/center-server/ apps/center-server/

RUN --mount=type=cache,id=gradle-home,target=/home/gradle/.gradle \
    gradle :apps:center-server:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache tini

ENV APIPRISM_STORAGE_DATA_DIR=/app/data
# Center 对外可访问的根地址，用于 Agent 文档中生成完整链接；为空时从请求头自动推导
ENV APIPRISM_CENTER_EXTERNAL_URL=

COPY --from=builder /workspace/apps/center-server/build/libs/*.jar /app/center-server.jar

RUN mkdir -p /app/data \
    && addgroup -S apiprism \
    && adduser -S -G apiprism -h /app apiprism \
    && chown -R apiprism:apiprism /app

VOLUME ["/app/data"]
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:8080/actuator/health >/dev/null || exit 1
USER apiprism
ENTRYPOINT ["tini", "--", "java", "-jar", "/app/center-server.jar"]
