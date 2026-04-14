# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS deps
WORKDIR /app

RUN apk add --no-cache gcompat

# 先复制依赖清单和 lock 文件，让依赖安装层可被缓存
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

FROM node:22-alpine AS builder
WORKDIR /app

RUN apk add --no-cache gcompat

COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN --mount=type=cache,id=pnpm-store,target=/pnpm/store \
    corepack enable \
    && pnpm config set store-dir /pnpm/store \
    && pnpm --filter @apiprism/center-web build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
RUN apk add --no-cache gcompat
COPY --from=builder /app/apps/center-web/.next/standalone ./
COPY --from=builder /app/apps/center-web/.next/static ./apps/center-web/.next/static
EXPOSE 3000
CMD ["node", "server.js"]
