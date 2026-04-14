<p align="center">
  <strong>English</strong> | <a href="./README_zh.md">中文</a>
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

**Every API deserves to be understood — by humans, by agents, by machines.**

A beam of light through a prism refracts into a full spectrum.
So does an API through APIPrism.

APIPrism is a lightweight, AI-native API platform for microservice teams. Add one Spring Boot
Starter dependency, and your service's OpenAPI spec is automatically pushed to a central catalog
on every startup — no manual uploads, no stale wikis. From there, APIPrism serves the same spec
in two forms: a browsable web UI for developers, and token-efficient Markdown slices that let AI
agents reason about, locate, and invoke your APIs without loading the full spec.

---

## Quick Start

### 1 — Start the Center

```bash
docker run -d \
  --name apiprism-center \
  -p 3000:3000 \
  -v apiprism-data:/app/data \
  --restart unless-stopped \
  apiprism/apiprism-center:latest
```

Open **http://localhost:3000** to confirm it's running.

> `/app/data` holds the embedded database and spec snapshots. Mount a named volume to persist data across restarts.

### 2 — Add the Starter

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

> Requires [springdoc-openapi](https://springdoc.org/) on the classpath — the starter reads `/v3/api-docs` automatically.

### 3 — Configure

```yaml
apiprism:
  center-url: http://localhost:3000   # APIPrism Center address
  project-name: my-service            # Shown in the catalog (falls back to spring.application.name)
  env: dev                            # Environment label (falls back to first active Spring profile)
```

That's it. On the next `ApplicationReadyEvent`, your API registers and appears in the catalog.

### 4 — Use It

| Output | URL |
|---|---|
| Web catalog | http://localhost:3000 |
| Services list (JSON) | http://localhost:3000/api/v1/services |
| Agent Markdown — all services | http://localhost:3000/apidocs.md |
| Agent Markdown — one service | http://localhost:3000/{service}/{env}/apidocs.md |
| Agent Markdown — one operation | http://localhost:3000/{service}/{env}/{operationId}/apidocs.md |

---

## Configuration Reference

| Property | Default | Description |
|---|---|---|
| `apiprism.center-url` | `http://localhost:8080` | **Required.** APIPrism Center address. |
| `apiprism.project-name` | *(spring.application.name)* | Service name in the catalog. |
| `apiprism.env` | *(first active profile or `default`)* | Environment label, e.g. `dev`, `staging`, `prod`. |
| `apiprism.enabled` | `true` | Set `false` to disable the adapter entirely. |
| `apiprism.register-on-startup` | `true` | Set `false` to skip auto-registration on startup. |
| `apiprism.openapi-path` | `/v3/api-docs` | Path to the OpenAPI JSON doc on your service. |
| `apiprism.server-urls` | *(auto-detected)* | Public URLs for this service. Auto-detected as `http://127.0.0.1:{port}` if empty. |
| `apiprism.retry.enabled` | `true` | Retry with exponential backoff on transient failures. |
| `apiprism.retry.max-attempts` | `15` | Max attempts (including first try). |
| `apiprism.retry.initial-interval-ms` | `3000` | Initial backoff interval (ms). |
| `apiprism.retry.multiplier` | `2.0` | Backoff multiplier per attempt. |
| `apiprism.retry.max-interval-ms` | `1800000` | Max backoff cap (30 minutes). |
| `apiprism.http-client.connect-timeout-ms` | `5000` | HTTP connect timeout (ms). |
| `apiprism.http-client.read-timeout-ms` | `10000` | HTTP read timeout (ms). |

---

## Center Image Reference

| Tag | Description |
|---|---|
| `latest` | Latest stable release |
| `0.1.0` | Pinned release |
| `sha-<commit>` | Immutable build from a specific commit |

**Environment variable:**

| Variable | Default | Description |
|---|---|---|
| `APIPRISM_STORAGE_DATA_DIR` | `/app/data` | Database and spec snapshot storage path. |

**Health check:** `GET http://localhost:3000/actuator/health`

---

## Self-Hosting with Docker Compose

```bash
git clone https://github.com/yipengfei329/apiprism.git
cd apiprism
docker compose -f deploy/docker-compose/docker-compose.yml up
```

---

## License

[Apache 2.0](LICENSE)
