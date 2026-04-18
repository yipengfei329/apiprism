# 部署指南：Vercel + Railway

APIPrism 拆成两个部署单元：

| 单元 | 运行时 | 托管方 | 说明 |
|---|---|---|---|
| `apps/center-server` | Spring Boot / JVM 21 | Railway | H2 文件数据库 + raw spec 归档挂载在持久卷 |
| `apps/center-web`    | Next.js (Node 22) | Vercel | 前端全部走 SSR/Server Action，通过 server-side fetch 调后端 |

浏览器只和 Vercel 通信，Vercel 的 Next.js 运行时再通过公网 HTTPS 调用 Railway。不涉及浏览器跨域，CORS 不用配。

## 一、Railway 部署 center-server

### 1. 新建 Project
1. 登录 [railway.com](https://railway.com) → **New Project** → **Deploy from GitHub repo**
2. 授权 Railway GitHub App 访问 `yipengfei329/apiprism`
3. 选 `main` 分支

### 2. 指定 Dockerfile
仓库根已有 `railway.json`，Railway 会自动读取：

```json
{
  "build": { "builder": "DOCKERFILE", "dockerfilePath": "apps/center-server/Dockerfile" },
  "deploy": {
    "healthcheckPath": "/actuator/health",
    "healthcheckTimeout": 100,
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 5
  }
}
```

如果 Railway 识别到 `apps/center-web/Dockerfile` 等其他 Dockerfile，去 **Service → Settings → Build** 确认 `Dockerfile Path` 是 `apps/center-server/Dockerfile`。

### 3. 挂载持久卷
1. **Service → Settings → Volumes** → **New Volume**
2. Mount Path: `/app/data`
3. 大小 1GB 起步足够（H2 文件 + raw spec 归档）

### 4. 环境变量
**Service → Variables** 添加：

| Key | Value | 说明 |
|---|---|---|
| `APIPRISM_STORAGE_DATA_DIR` | `/app/data` | H2 文件 + raw spec 目录，必须与挂载路径一致 |
| `APIPRISM_CENTER_EXTERNAL_URL` | `https://<vercel-domain>` | 选填。Agent 文档里要用前端域名拼 URL 时需要；缺省时从 `X-Forwarded-*` 头推导（Vercel proxy 已经传了） |

### 5. 暴露公网端口
**Service → Settings → Networking** → **Generate Domain**，拿到形如 `center-server-production-xxxx.up.railway.app` 的公网地址。记下来，Vercel 那边要用。

### 6. 首次部署
Railway 会在挂载 Volume + 环境变量配好后自动 rebuild。`/actuator/health` 通过才算健康。日志里出现 `Started CenterServerApplication` 代表就绪。

## 二、Vercel 部署 center-web

### 1. 导入 Project
1. 登录 [vercel.com](https://vercel.com) → **Add New** → **Project**
2. 授权 GitHub App 并导入 `yipengfei329/apiprism`

### 2. 关键配置
Vercel 识别 `apps/center-web/vercel.json` 里的 `framework: nextjs`，但需要在 UI 里告诉它去哪个目录：

| 字段 | 值 |
|---|---|
| **Root Directory** | `apps/center-web` |
| **Framework Preset** | Next.js（自动） |
| **Build Command** | 留空，让 Vercel 自动检测 pnpm workspace |
| **Install Command** | 留空，同上 |
| **Output Directory** | 留空（`.next` 由 preset 处理） |

> Vercel 检测到仓库根的 `pnpm-workspace.yaml` 后会从 workspace 根跑 `pnpm install`，同时正确 resolve `@apiprism/center-web` 的 workspace 依赖。如果 build 失败找不到依赖，手动把 Install Command 设为 `pnpm install --frozen-lockfile --filter @apiprism/center-web...`。

### 3. 环境变量
**Project → Settings → Environment Variables**，只加 Production（单环境够用）：

| Key | Value | 说明 |
|---|---|---|
| `APIPRISM_SERVER_INTERNAL_BASE` | `https://<railway-domain>` | 第 5 步记下的 Railway 公网地址，必须带 `https://`，不带结尾 `/` |

前端所有 `app/lib/internal-api.ts` 和 `app/lib/proxy.ts` 都读这一个变量。

### 4. 部署
Vercel 自动触发首次 Build。完成后拿到 `*.vercel.app` 域名。如果想绑自己的域名，**Settings → Domains** 添加。

## 三、联动自动部署

两边都已接到 GitHub：

- `main` 分支 push 或 merge → Railway 自动构建新镜像 + 滚动发布；Vercel 自动 build + 替换生产域名
- PR 分支 → **Vercel 会出 Preview URL**（独立域名，每个 commit 一个）；Railway 这边保持单环境，不做 PR Preview
- Vercel 的 Preview 环境默认继承 Production 环境变量（包括 `APIPRISM_SERVER_INTERNAL_BASE`），所以 PR Preview 的前端也打同一个 Railway 后端——够用，注意不要在 preview 里造脏数据

## 四、CI 门槛

仓库已接入 `.github/workflows/ci.yml`：PR 和 push main 都会跑 `./gradlew test`。Java 测试挂掉会阻塞合并。

前端 lint (`pnpm lint:web`) 暂未入 CI。想加的话把这段追加进 `ci.yml`：

```yaml
  web-lint:
    name: Web lint
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: pnpm/action-setup@v4
        with: { version: 10 }
      - uses: actions/setup-node@v4
        with: { node-version: "22", cache: pnpm }
      - run: pnpm install --frozen-lockfile --filter @apiprism/center-web...
      - run: pnpm lint:web
```

当前 HEAD 有 3 个预存的 lint 错（`SidebarLayout.tsx`），加 CI 前先修掉，否则 main 直接红。

## 五、故障排查

| 症状 | 可能原因 | 处理 |
|---|---|---|
| Vercel 前端 500，日志 `ECONNREFUSED 127.0.0.1:8080` | `APIPRISM_SERVER_INTERNAL_BASE` 没配或值错 | 检查 Vercel env 和值末尾有无多余斜杠 |
| Railway 每次重启数据丢失 | 没挂 Volume 或 `APIPRISM_STORAGE_DATA_DIR` 指向非挂载目录 | 重新挂 Volume + 检查 env |
| Railway healthcheck 失败 | JVM 启动慢过了超时 | 在 Railway 把 `healthcheckTimeout` 调到 180 |
| Agent 文档里 URL 是 Railway 地址不是 Vercel 域名 | `APIPRISM_CENTER_EXTERNAL_URL` 未配 | 在 Railway 设置为 Vercel 的公网域名 |
| Vercel Build 失败在 `pnpm install` | Root Directory 设成了仓库根而不是 `apps/center-web` | 改回 `apps/center-web`，Vercel 会自动上溯识别 pnpm workspace |

## 六、费用参考（2026 年初）

- **Railway**：Hobby plan $5/月包含 $5 用量 credit。center-server 空载内存约 300MB、CPU 很低，估算 $3~5/月。
- **Vercel**：Hobby plan 免费，个人项目、非商用、带宽 100GB/月够用。若有团队或商用需 Pro（$20/用户/月）。
- **H2 文件数据库**：零成本，数据量大到千万级再考虑换 Postgres。
