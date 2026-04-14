<p align="center">
  <a href="./README.md">English</a> | <strong>中文</strong>
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/ai.apiprism/apiprism-spring-boot-starter">
    <img src="https://img.shields.io/maven-central/v/ai.apiprism/apiprism-spring-boot-starter?label=starter" alt="Maven Central">
  </a>
  <a href="https://hub.docker.com/r/apiprism/apiprism-center">
    <img src="https://img.shields.io/docker/pulls/apiprism/apiprism-center?label=docker%20pulls" alt="Docker Pulls">
  </a>
  <a href="https://github.com/yipengfei329/apiprism/actions/workflows/docker-publish.yml">
    <img src="https://github.com/yipengfei329/apiprism/actions/workflows/docker-publish.yml/badge.svg" alt="Build">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-Apache%202.0-blue.svg" alt="License">
  </a>
  <img src="https://img.shields.io/badge/Java-17%2B-orange" alt="Java 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6db33f" alt="Spring Boot 3">
</p>

# APIPrism

**一份 API，折射完整光谱。**

一束光经过棱镜，折射出完整色谱。一份 API 经过 APIPrism，亦然。

APIPrism 是面向微服务团队的轻量级 AI 原生 API 平台。引入一个 Spring Boot Starter 依赖，
服务在每次启动时自动将 OpenAPI 规范推送至统一目录 — 无需手动上传，告别过期文档。
同一份规范折射为两种形态：供开发者浏览的交互式文档，以及供 AI Agent 推理、
定位并调用接口的 Markdown 切片 — 无需加载完整规范，开箱即用。

---

## 快速开始

### 第一步 — 启动 Center

```bash
docker run -d \
  --name apiprism-center \
  -p 3000:3000 \
  -v apiprism-data:/app/data \
  --restart unless-stopped \
  apiprism/apiprism-center:latest
```

打开 **http://localhost:3000**，确认服务正常运行。

> `/app/data` 存储嵌入式数据库和规范快照。挂载命名卷可在容器重启后保留数据。

### 第二步 — 引入 Starter

**Gradle（Kotlin DSL）**
```kotlin
implementation("ai.apiprism:apiprism-spring-boot-starter:0.1.0")
```

**Gradle（Groovy）**
```groovy
implementation 'ai.apiprism:apiprism-spring-boot-starter:0.1.0'
```

**Maven**
```xml
<dependency>
  <groupId>ai.apiprism</groupId>
  <artifactId>apiprism-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

> 需要在 classpath 中引入 [springdoc-openapi](https://springdoc.org/)，Starter 会自动读取 `/v3/api-docs`。

### 第三步 — 配置

```yaml
apiprism:
  center-url: http://localhost:3000   # APIPrism Center 地址
  project-name: my-service            # 目录中显示的服务名（默认取 spring.application.name）
  env: dev                            # 环境标签（默认取第一个激活的 Spring profile）
```

完成。下次 `ApplicationReadyEvent` 触发时，服务 API 自动注册并出现在目录中。

### 第四步 — 使用

| 输出形态 | 地址 |
|---|---|
| Web 目录 | http://localhost:3000 |
| 服务列表（JSON） | http://localhost:3000/api/v1/services |
| Agent Markdown（全量） | http://localhost:3000/apidocs.md |
| Agent Markdown（单服务） | http://localhost:3000/{service}/{env}/apidocs.md |
| Agent Markdown（单接口） | http://localhost:3000/{service}/{env}/{operationId}/apidocs.md |

---

## 配置参考

| 属性 | 默认值 | 说明 |
|---|---|---|
| `apiprism.center-url` | `http://localhost:8080` | **必填。** APIPrism Center 地址。 |
| `apiprism.project-name` | *(spring.application.name)* | 目录中显示的服务名。 |
| `apiprism.env` | *(第一个激活的 profile 或 `default`)* | 环境标签，如 `dev`、`staging`、`prod`。 |
| `apiprism.enabled` | `true` | 设为 `false` 可完全禁用适配器。 |
| `apiprism.register-on-startup` | `true` | 设为 `false` 可跳过启动时自动注册。 |
| `apiprism.openapi-path` | `/v3/api-docs` | 服务上 OpenAPI JSON 文档的路径。 |
| `apiprism.server-urls` | *(自动探测)* | 服务公开地址，为空时自动探测为 `http://127.0.0.1:{port}`。 |
| `apiprism.retry.enabled` | `true` | 注册失败时启用指数退避重试。 |
| `apiprism.retry.max-attempts` | `15` | 最大尝试次数（含首次）。 |
| `apiprism.retry.initial-interval-ms` | `3000` | 初始退避间隔（毫秒）。 |
| `apiprism.retry.multiplier` | `2.0` | 每次重试的退避倍数。 |
| `apiprism.retry.max-interval-ms` | `1800000` | 退避上限（30 分钟）。 |
| `apiprism.http-client.connect-timeout-ms` | `5000` | HTTP 连接超时（毫秒）。 |
| `apiprism.http-client.read-timeout-ms` | `10000` | HTTP 读取超时（毫秒）。 |

---

## Center 镜像参考

| 标签 | 说明 |
|---|---|
| `latest` | 最新稳定版 |
| `0.1.0` | 固定版本 |
| `sha-<commit>` | 来自特定 commit 的不可变构建 |

**环境变量：**

| 变量 | 默认值 | 说明 |
|---|---|---|
| `APIPRISM_STORAGE_DATA_DIR` | `/app/data` | 数据库和规范快照的存储路径。 |

**健康检查：** `GET http://localhost:3000/actuator/health`

---

## Docker Compose 自托管

```bash
git clone https://github.com/yipengfei329/apiprism.git
cd apiprism
docker compose -f deploy/docker-compose/docker-compose.yml up
```

---

## License

[Apache 2.0](LICENSE)
