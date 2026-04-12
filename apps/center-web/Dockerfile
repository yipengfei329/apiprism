FROM node:22-alpine AS deps
WORKDIR /app
# 先复制依赖清单和 lock 文件，让依赖安装层可被缓存
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/center-web/package.json apps/center-web/package.json
RUN corepack enable && pnpm install --filter @apiprism/center-web... --frozen-lockfile

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN corepack enable && pnpm --filter @apiprism/center-web build

FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
COPY --from=builder /app/apps/center-web/.next/standalone ./
COPY --from=builder /app/apps/center-web/.next/static ./apps/center-web/.next/static
EXPOSE 3000
CMD ["node", "server.js"]
