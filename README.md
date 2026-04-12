<p align="center">
  <strong>English</strong> | <a href="./README_zh.md">中文</a>
</p>

# APIPrism

**Make every API in your organization discoverable — by humans and AI agents alike.**

APIPrism is a lightweight API catalog that automatically collects OpenAPI specs from running services, normalizes them into a unified model, and serves them as both a browsable UI and agent-friendly Markdown — so LLMs can understand and call your APIs without manual prompt engineering.

## Why

Microservice teams produce dozens of OpenAPI specs scattered across repos and runtime endpoints. Developers search Confluence, Slack, or source code to find the right API. AI agents have it worse — they need structured, token-efficient descriptions they can act on.

APIPrism solves both: drop in a starter dependency, and your service's API surfaces in a central catalog within seconds of boot.

## How It Works

```
┌─────────────────┐         register          ┌──────────────────┐
│  Your Service   │  ───── OpenAPI JSON ─────▶ │   APIPrism       │
│  + starter dep  │      (on app ready)        │   Center Server  │
└─────────────────┘                            └────────┬─────────┘
                                                        │
                                         ┌──────────────┼──────────────┐
                                         ▼              ▼              ▼
                                   Query APIs     Web Catalog    Agent Markdown
                                  (JSON REST)     (Next.js UI)   (LLM-ready)
```

1. **Adapter** — A Spring Boot Starter that reads your service's OpenAPI doc on startup and pushes it to the center.
2. **Center Server** — Receives registrations, normalizes specs into a canonical model, stores snapshots, and exposes query + rendering APIs.
3. **Center Web** — A Next.js frontend for browsing services, groups, and operations.
4. **Agent Markdown** — Renders each operation as Markdown aligned with LLM function-call conventions (operationId, JSON Schema params, curl examples).

## Quick Start

### 1. Start the Center

```bash
docker compose -f deploy/docker-compose/docker-compose.yml up --build
```

Or run locally:

```bash
./gradlew :apps:center-server:bootRun
```

### 2. Integrate Your Service

Add the starter to your Spring Boot app:

```gradle
implementation 'ai.apiprism:apiprism-spring-boot-starter:0.1.0-SNAPSHOT'
```

Configure the center endpoint:

```yaml
apiprism:
  center:
    url: http://localhost:8080
```

That's it. On application ready, your API is registered.

### 3. Browse

- **Web UI**: `http://localhost:3000`
- **Service list API**: `GET /api/v1/services`
- **Agent Markdown**: `GET /api/v1/services/{name}/markdown`

## Project Structure

```
adapters/java/starter   → Spring Boot Starter (auto-registration)
apps/center-server      → Registration, normalization, query & Markdown APIs
apps/center-web         → Next.js catalog UI
libs/api-model          → Canonical API model
libs/openapi-parser     → OpenAPI → canonical model transformer
libs/registration-protocol → Adapter ↔ Center DTO contract
examples/java-demo-service → Integration demo
```

## Roadmap

- [ ] Persistent storage (currently in-memory)
- [ ] Multi-language adapters (Go, Python, Node.js)
- [ ] Diff & changelog between spec versions
- [ ] MCP server integration for AI agent tool-use
- [ ] Authentication & multi-tenant support

## Tech Stack

Java 17+ / Spring Boot 3 / Gradle — Next.js / Tailwind CSS / pnpm

## License

MIT
