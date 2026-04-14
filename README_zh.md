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

## 快速开始

### 1. 启动 Center

```bash
docker compose -f deploy/docker-compose/docker-compose.yml up --build
```

启动后访问：

- Web UI：`http://localhost:3000`
- 服务列表 API：`http://localhost:3000/api/v1/services`
- Agent Markdown：`http://localhost:3000/api/v1/apidocs.md`

只构建单镜像运行：

```bash
docker build -f deploy/center-image/Dockerfile -t apiprism-center:local .
docker run -d \
  --name apiprism-center \
  -p 3000:3000 \
  -v apiprism-center-data:/app/data \
  --restart unless-stopped \
  apiprism-center:local
```

或本地运行：

```bash
./gradlew :apps:center-server:bootRun
```

### 2. 接入你的服务

在 Spring Boot 应用中添加 Starter：

```gradle
implementation 'ai.apiprism:apiprism-spring-boot-starter:0.1.0-SNAPSHOT'
```

配置 Center 地址：

```yaml
apiprism:
  center:
    url: http://localhost:8080
```

完成。应用启动后 API 自动注册。

### 3. 浏览

- **统一入口**：`http://localhost:3000`
- **服务列表 API**：`GET /api/v1/services`
- **Agent Markdown**：`GET /api/v1/services/{name}/markdown`

## 运行说明

- `center-server` 容器默认监听 `8080` 端口。
- 官方交付镜像对外仅暴露 `3000` 端口，由 `center-web` 作为统一入口代理后端 API。
- 默认数据目录是容器内 `/app/data`，会持久化 H2 数据库、规范快照和原始 OpenAPI 文档。
- 可通过环境变量 `APIPRISM_STORAGE_DATA_DIR` 覆盖数据目录。
- 健康检查地址为 `GET /actuator/health`，通过统一入口访问即可。

## 项目结构

```
adapters/java/starter   → Spring Boot Starter（自动注册）
apps/center-server      → 注册、规范化、查询与 Markdown API
apps/center-web         → Next.js 目录 UI
libs/api-model          → 内部统一 API 模型
libs/openapi-parser     → OpenAPI → 内部模型转换器
libs/registration-protocol → 适配器 ↔ Center DTO 协议
examples/java-demo-service → 集成示例
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
