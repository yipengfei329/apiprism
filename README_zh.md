<p align="center">
  <a href="./README.md">English</a> | <strong>中文</strong>
</p>

# APIPrism

**让组织内的每一个 API 都可被发现 — 无论是人还是 AI Agent。**

APIPrism 是一个轻量级 API 目录平台，能够自动从运行中的服务收集 OpenAPI 规范，将其规范化为统一模型，同时提供可浏览的 Web UI 和面向 Agent 的 Markdown 输出 — 让 LLM 无需手工 prompt engineering 即可理解和调用你的 API。

## 为什么需要 APIPrism

微服务团队产出大量 OpenAPI 规范，散落在各个仓库和运行时端点中。开发者不得不在 Confluence、Slack 或源码中搜索正确的接口。AI Agent 面临更大的挑战 — 它们需要结构化、token 高效的描述才能执行调用。

APIPrism 同时解决这两个问题：引入一个 Starter 依赖，你的服务 API 在启动后数秒内即出现在统一目录中。

## 工作原理

```
┌─────────────────┐         注册              ┌──────────────────┐
│  你的服务        │  ───── OpenAPI JSON ─────▶ │   APIPrism       │
│  + starter 依赖  │      (应用就绪时)          │   Center Server  │
└─────────────────┘                            └────────┬─────────┘
                                                        │
                                         ┌──────────────┼──────────────┐
                                         ▼              ▼              ▼
                                    查询 API       Web 目录      Agent Markdown
                                   (JSON REST)    (Next.js)     (LLM 友好)
```

1. **适配器** — Spring Boot Starter，在应用启动时读取 OpenAPI 文档并推送至 Center。
2. **Center Server** — 接收注册、规范化为内部模型、存储快照、暴露查询与渲染 API。
3. **Center Web** — Next.js 前端，浏览服务、分组和接口。
4. **Agent Markdown** — 将每个接口渲染为对齐 LLM function-call 约定的 Markdown（operationId、JSON Schema 参数、curl 示例）。

---

## 快速开始

### 第一步 — 启动 Center

从 Docker Hub 拉取并运行官方镜像：

```bash
docker run -d \
  --name apiprism-center \
  -p 3000:3000 \
  -v apiprism-data:/app/data \
  --restart unless-stopped \
  yipengfei329/apiprism-center:latest
```

容器健康后，打开 **http://localhost:3000**，即可看到 APIPrism 目录 UI。

> **数据持久化** — `/app/data` 存储嵌入式数据库和规范快照。
> 挂载宿主目录或命名卷可保留容器重启后的数据。

---

### 第二步 — 在服务中引入 Starter

Adapter 已发布至 Maven 中央仓库。

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

> **前置要求** — 你的服务需通过 [springdoc-openapi](https://springdoc.org/) 暴露 OpenAPI 文档，
> 默认路径 `/v3/api-docs` 会被自动使用。

---

### 第三步 — 配置 Center 地址

在服务的 `application.yml` 中添加：

```yaml
apiprism:
  center-url: http://localhost:3000   # APIPrism Center 地址
  project-name: my-service            # 目录中显示的服务名（默认取 spring.application.name）
  env: dev                            # 环境标签（默认取第一个激活的 Spring profile）
```

完成。下次 `ApplicationReadyEvent` 触发时，你的 API 即自动注册并出现在目录中。

---

### 第四步 — 验证结果

| 内容 | 地址 |
|---|---|
| Web 目录 | http://localhost:3000 |
| 服务列表（JSON） | http://localhost:3000/api/v1/services |
| Agent Markdown（全量） | http://localhost:3000/apidocs.md |
| Agent Markdown（单服务） | http://localhost:3000/{service}/{env}/apidocs.md |

---

## 配置参考

所有属性位于 `apiprism.*` 命名空间下，除特别说明外均为可选。

| 属性 | 默认值 | 说明 |
|---|---|---|
| `apiprism.center-url` | `http://localhost:8080` | **必填。** APIPrism Center 地址。 |
| `apiprism.project-name` | *(spring.application.name)* | 目录中显示的服务名。 |
| `apiprism.env` | *(第一个激活的 profile 或 `default`)* | 环境标签，如 `dev`、`staging`、`prod`。 |
| `apiprism.enabled` | `true` | 设为 `false` 可完全禁用适配器。 |
| `apiprism.register-on-startup` | `true` | 设为 `false` 可跳过启动时自动注册。 |
| `apiprism.openapi-path` | `/v3/api-docs` | 服务上 OpenAPI JSON 文档的路径。 |
| `apiprism.server-urls` | *(自动探测)* | 服务的公开访问地址，为空时自动探测为 `http://127.0.0.1:{port}`。 |
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
| `sha-<git-sha>` | 来自特定 commit 的不可变构建 |

**Center 容器的环境变量：**

| 变量 | 默认值 | 说明 |
|---|---|---|
| `APIPRISM_STORAGE_DATA_DIR` | `/app/data` | 嵌入式数据库和规范快照的存储目录。 |

**健康检查：** `GET http://localhost:3000/actuator/health`

---

## 使用 Docker Compose 自托管

适用于本地开发或私有化部署，仓库内已包含现成的 Compose 文件：

```bash
git clone https://github.com/yipengfei329/apiprism.git
cd apiprism
docker compose -f deploy/docker-compose/docker-compose.yml up
```

---

## 项目结构

```
adapters/java/starter        → Spring Boot Starter（自动注册）
apps/center-server           → 注册、规范化、查询与 Markdown API
apps/center-web              → Next.js 目录 UI
libs/api-model               → 内部统一 API 模型
libs/openapi-parser          → OpenAPI → 内部模型转换器
libs/registration-protocol   → 适配器 ↔ Center DTO 协议
examples/java-demo-service   → 集成示例
```

## Roadmap

- [ ] 外部数据库支持（PostgreSQL / MySQL）
- [ ] 多语言适配器（Go、Python、Node.js）
- [ ] 规范版本 Diff 与变更日志
- [ ] MCP Server 集成，支持 AI Agent tool-use
- [ ] 认证与多租户支持

## 技术栈

Java 17+ / Spring Boot 3 / Gradle — Next.js / Tailwind CSS / pnpm

## License

MIT
