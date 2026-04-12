# APIPrism Architecture

## Layers

- `adapters/*`: language-specific ingress products that discover local OpenAPI data and register it with the center.
- `platform/contracts`: shared protocol boundary between adapters and the center.
- `platform/canonical-model`: stable internal model that powers every downstream representation.
- `platform/normalization`: OpenAPI parsing and transformation pipeline.
- `apps/center-server`: registration, storage, queries, and Markdown rendering.
- `apps/center-web`: human-oriented UI built on the center query APIs.

## Current Java Adapter Flow

1. Spring Boot app includes `apiprism-spring-boot-starter`.
2. Starter ensures the SpringDoc API endpoint exists through transitive dependency.
3. On `ApplicationReadyEvent`, the starter reads `/v3/api-docs`.
4. The starter pushes the document to the center with service metadata.
5. The center stores raw spec content and builds a canonical snapshot.

## Center Pipeline

1. Receive registration payload.
2. Persist current raw spec and metadata in memory.
3. Normalize the spec into service/group/operation structures.
4. Expose query APIs for the frontend.
5. Expose service/group/operation Markdown for agent consumption.
