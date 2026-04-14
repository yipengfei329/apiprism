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

---

## Quick Start

### Step 1 — Start the Center

Pull and run the official image from Docker Hub:

```bash
docker run -d \
  --name apiprism-center \
  -p 3000:3000 \
  -v apiprism-data:/app/data \
  --restart unless-stopped \
  yipengfei329/apiprism-center:latest
```

Once healthy, open **http://localhost:3000** — you should see the APIPrism catalog UI.

> **Data persistence** — `/app/data` holds the embedded database and spec snapshots.
> Mount a host volume or named volume to keep data across container restarts.

---

### Step 2 — Add the Starter to Your Service

The adapter is published to Maven Central.

**Gradle (Kotlin DSL)**
```kotlin
implementation("ai.apiprism:apiprism-spring-boot-starter:0.1.0")
```

**Gradle (Groovy)**
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

> **Prerequisite** — Your service must expose an OpenAPI spec via [springdoc-openapi](https://springdoc.org/)
> (the default path `/v3/api-docs` is used automatically).

---

### Step 3 — Configure the Center URL

Add the following to your service's `application.yml`:

```yaml
apiprism:
  center-url: http://localhost:3000   # URL of the APIPrism Center
  project-name: my-service            # Displayed in the catalog (falls back to spring.application.name)
  env: dev                            # Environment label (falls back to the first active Spring profile)
```

That's it. On the next `ApplicationReadyEvent`, your API is registered and appears in the catalog.

---

### Step 4 — Verify

| What | URL |
|---|---|
| Web catalog | http://localhost:3000 |
| All services (JSON) | http://localhost:3000/api/v1/services |
| Agent Markdown (all) | http://localhost:3000/apidocs.md |
| Agent Markdown (service) | http://localhost:3000/{service}/{env}/apidocs.md |

---

## Configuration Reference

All properties are under the `apiprism.*` namespace and are optional unless noted.

| Property | Default | Description |
|---|---|---|
| `apiprism.center-url` | `http://localhost:8080` | **Required.** APIPrism Center address. |
| `apiprism.project-name` | *(spring.application.name)* | Service name shown in the catalog. |
| `apiprism.env` | *(first active profile or `default`)* | Environment label (e.g. `dev`, `staging`, `prod`). |
| `apiprism.enabled` | `true` | Set to `false` to disable the adapter entirely. |
| `apiprism.register-on-startup` | `true` | Set to `false` to skip auto-registration on startup. |
| `apiprism.openapi-path` | `/v3/api-docs` | Path to the OpenAPI JSON doc on your service. |
| `apiprism.server-urls` | *(auto-detected)* | Public URLs for this service. Auto-detected as `http://127.0.0.1:{port}` if empty. |
| `apiprism.retry.enabled` | `true` | Retry registration with exponential backoff on transient failures. |
| `apiprism.retry.max-attempts` | `15` | Max attempts including the initial try. |
| `apiprism.retry.initial-interval-ms` | `3000` | Initial backoff interval (ms). |
| `apiprism.retry.multiplier` | `2.0` | Backoff multiplier per attempt. |
| `apiprism.retry.max-interval-ms` | `1800000` | Maximum backoff cap (30 minutes). |
| `apiprism.http-client.connect-timeout-ms` | `5000` | HTTP connect timeout (ms). |
| `apiprism.http-client.read-timeout-ms` | `10000` | HTTP read timeout (ms). |

---

## Center Image Reference

| Tag | Description |
|---|---|
| `latest` | Latest stable release |
| `0.1.0` | Pinned release |
| `sha-<git-sha>` | Immutable build from a specific commit |

**Environment variables for the center container:**

| Variable | Default | Description |
|---|---|---|
| `APIPRISM_STORAGE_DATA_DIR` | `/app/data` | Data directory for the embedded database and spec snapshots. |

**Health check:** `GET http://localhost:3000/actuator/health`

---

## Self-Hosting with Docker Compose

For local development or on-premise deployment, a ready-made Compose file is included:

```bash
git clone https://github.com/yipengfei329/apiprism.git
cd apiprism
docker compose -f deploy/docker-compose/docker-compose.yml up
```

---

## Project Structure

```
adapters/java/starter        → Spring Boot Starter (auto-registration)
apps/center-server           → Registration, normalization, query & Markdown APIs
apps/center-web              → Next.js catalog UI
libs/api-model               → Canonical API model
libs/openapi-parser          → OpenAPI → canonical model transformer
libs/registration-protocol   → Adapter ↔ Center DTO contract
examples/java-demo-service   → Integration demo
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
