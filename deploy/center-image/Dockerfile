# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS web-deps
WORKDIR /workspace

RUN apk add --no-cache gcompat

COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY scripts/ scripts/
COPY apps/center-web/package.json apps/center-web/package.json
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store \
    corepack enable \
    && pnpm config set network-concurrency 1 \
    && pnpm config set fetch-retries 5 \
    && pnpm config set fetch-retry-mintimeout 10000 \
    && pnpm config set fetch-retry-maxtimeout 120000 \
    && pnpm config set store-dir /pnpm/store \
    && pnpm install --filter @apiprism/center-web... --frozen-lockfile

FROM node:22-alpine AS web-builder
WORKDIR /workspace

RUN apk add --no-cache gcompat

COPY --from=web-deps /workspace/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store \
    corepack enable \
    && pnpm config set store-dir /pnpm/store \
    && pnpm --filter @apiprism/center-web build

FROM gradle:9.4.1-jdk21 AS server-builder
WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle/ gradle/
COPY libs/registration-protocol/build.gradle.kts    libs/registration-protocol/
COPY libs/api-model/build.gradle.kts                libs/api-model/
COPY libs/openapi-parser/build.gradle.kts           libs/openapi-parser/
COPY adapters/java/starter/build.gradle.kts         adapters/java/starter/
COPY examples/java-demo-service/build.gradle.kts    examples/java-demo-service/
COPY apps/center-server/build.gradle.kts            apps/center-server/

RUN --mount=type=cache,id=gradle-home,target=/home/gradle/.gradle \
    gradle :apps:center-server:dependencies --no-daemon

COPY libs/ libs/
COPY adapters/ adapters/
COPY examples/ examples/
COPY apps/center-server/ apps/center-server/

RUN --mount=type=cache,id=gradle-home,target=/home/gradle/.gradle \
    gradle :apps:center-server:bootJar --no-daemon

FROM node:22-alpine AS runtime
WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
ENV APIPRISM_SERVER_INTERNAL_BASE=http://127.0.0.1:8080
ENV APIPRISM_STORAGE_DATA_DIR=/app/data

RUN apk add --no-cache \
        gcompat \
        openjdk21-jre-headless \
        tini

COPY --from=web-builder /workspace/apps/center-web/.next/standalone /app/center-web
COPY --from=web-builder /workspace/apps/center-web/.next/static /app/center-web/apps/center-web/.next/static
COPY --from=server-builder /workspace/apps/center-server/build/libs/*.jar /app/center-server.jar
COPY deploy/center-image/start.sh /usr/local/bin/start-apiprism-center.sh

RUN chmod +x /usr/local/bin/start-apiprism-center.sh \
    && mkdir -p /app/data \
    && addgroup -S apiprism \
    && adduser -S -G apiprism -h /app apiprism \
    && chown -R apiprism:apiprism /app /usr/local/bin/start-apiprism-center.sh

VOLUME ["/app/data"]
EXPOSE 3000
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD wget -qO- http://127.0.0.1:3000/actuator/health >/dev/null || exit 1

USER apiprism
ENTRYPOINT ["tini", "--", "/usr/local/bin/start-apiprism-center.sh"]
