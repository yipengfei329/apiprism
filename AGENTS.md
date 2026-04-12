# APIPrism Repository Guidelines

## Module Map

Monorepo structure:

| Path | Role |
|---|---|
| `apps/center-server` | Spring Boot 后端：注册、规范化、查询 API、Markdown 输出 |
| `apps/center-web` | Next.js 前端：API 目录 UI |
| `adapters/java/core` | Java 注册客户端与适配器运行时 |
| `adapters/java/starter` | Spring Boot Starter，作为可复用适配器包发布 |
| `libs/registration-protocol` | 适配器↔Center 注册协议 DTO |
| `libs/api-model` | Center 内部 API 模型（存储、查询、渲染） |
| `libs/openapi-parser` | OpenAPI → 内部 API 模型的解析转换 |
| `examples/java-demo-service` | 适配器集成和复杂参数场景验证 Demo |
| `deploy/docker-compose` | Center 本地容器编排 |
| `docs` | 架构与贡献者文档 |

- Java: `src/main/java` / `src/test/java`。前端: `apps/center-web/app`。
- `libs/*` 是共享库层，不是应用入口。仅当逻辑属于共享边界或共享转换规则时才放入 `libs/*`，其余归 `apps/*` 或 `adapters/*`。

## Commands

| Command | Purpose |
|---|---|
| `./gradlew test --no-daemon` | 运行所有 Java 测试 |
| `./gradlew :apps:center-server:bootRun` | 本地启动后端 |
| `pnpm install` | 安装前端依赖 |
| `pnpm build:web` | 生产构建 Next.js |
| `pnpm lint:web` | ESLint 检查 center-web |
| `docker compose -f deploy/docker-compose/docker-compose.yml up --build` | 本地运行 Center 全栈 |

优先使用最小化验证命令；跨模块变更时提交前做全量验证。

## Coding Style

**Java**: 4-space 缩进，`PascalCase` 类型，`camelCase` 方法/字段，包根 `ai.apiprism`。Spring 模块按 controller / service / config 分包。数据对象优先用 Lombok 注解。

**TypeScript/React**: 2-space 缩进，`PascalCase` 组件，`camelCase` 函数/变量。center-web 优先 Tailwind，全局 CSS 仅限主题 token 和 base style。ESLint 校验。

**通用**:
- 代码注释用中文，运行时日志用英文。
- 关键分支、外部调用、失败和状态变迁处加结构化日志，便于线上问题诊断。
- 不提交密钥或环境特定 URL；敏感值（token、凭据、鉴权头）禁止出现在日志中。
- 适配器↔Center 配置通过 properties / 环境变量外部化。
- OpenAPI 载荷视为不可信输入，校验放在 Center 接收路径。

## Testing

- JUnit 5 + Spring Boot Test。测试文件命名 `*Test.java`，就近放置。
- 新增规范化逻辑、注册行为或 Markdown 渲染时至少补一个聚焦单测。
- 提 PR 前运行 `./gradlew test --no-daemon` 和 `pnpm lint:web`。

## Commits & PRs

格式: Conventional Commits `type(scope): summary`。

- Types: `feat` / `fix` / `docs` / `refactor` / `test` / `build` / `ci` / `chore`
- Scopes: `center-server` / `center-web` / `starter` / `java-core` / `registration-protocol` / `api-model` / `openapi-parser`
- 不兼容变更用 `!` 或 `BREAKING CHANGE:` footer。
- commitlint 校验，Git hooks 位于 `.githooks/commit-msg`。
- 每个 commit 聚焦单一关注点。
- PR 包含：变更摘要、涉及模块、已执行的验证步骤、前端变更附截图。

## Claude Code Notes

- 个人偏好放 `CLAUDE.local.md`，不要写进本文件。
