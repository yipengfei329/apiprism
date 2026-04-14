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

Then visit:

- Web UI: `http://localhost:3000`
- Service list API: `http://localhost:3000/api/v1/services`
- Agent Markdown: `http://localhost:3000/api/v1/apidocs.md`

Build and run the single distribution image directly:

```bash
docker build -f deploy/center-image/Dockerfile -t apiprism-center:local .
docker run -d \
  --name apiprism-center \
  -p 3000:3000 \
  -v apiprism-center-data:/app/data \
  --restart unless-stopped \
  apiprism-center:local
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

- **Single entrypoint**: `http://localhost:3000`
- **Service list API**: `GET /api/v1/services`
- **Agent Markdown**: `GET /api/v1/services/{name}/markdown`

## Runtime Notes

- The official distribution image exposes only port `3000`, with `center-web` acting as the single entrypoint and proxying backend API traffic.
- The default data directory is `/app/data`, which persists the embedded H2 database, normalized snapshots, and raw OpenAPI specs.
- Override the data directory with `APIPRISM_STORAGE_DATA_DIR` when needed.
- The container health endpoint is `GET /actuator/health`, reachable through the single entrypoint.

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

- [ ] External database support (PostgreSQL / MySQL)
- [ ] Multi-language adapters (Go, Python, Node.js)
- [ ] Diff & changelog between spec versions
- [ ] MCP server integration for AI agent tool-use
- [ ] Authentication & multi-tenant support

## Tech Stack

Java 17+ / Spring Boot 3 / Gradle — Next.js / Tailwind CSS / pnpm

## License

MIT
