import Link from "next/link";
import { getInternalApiUrl } from "@/app/lib/internal-api";


type GroupRef = {
  name: string;
  slug: string;
};

type ServiceCatalogItem = {
  name: string;
  environment: string;
  title: string;
  version: string;
  updatedAt: string;
  groups: GroupRef[];
};

async function getServices(): Promise<ServiceCatalogItem[]> {
  try {
    const response = await fetch(getInternalApiUrl("/api/v1/services"), { cache: "no-store" });
    if (!response.ok) return [];
    return response.json();
  } catch {
    return [];
  }
}

function formatDate(iso: string): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return "";
  return d.toLocaleDateString("zh-CN", { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" });
}

/* 三道光谱的配置 */
const spectrum = [
  {
    hex: "#7B2FBE",
    bg: "rgba(123, 47, 190, 0.05)",
    title: "开发者文档",
    desc: "美观、可交互的分组文档页面 — 按服务、环境、接口逐层展开，参数与示例一目了然。",
    cta: "浏览文档",
    href: "/docs",
    external: false,
    icon: (
      <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
        <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
      </svg>
    ),
  },
  {
    hex: "#0063CC",
    bg: "rgba(0, 99, 204, 0.05)",
    title: "Agent Markdown",
    desc: "语义丰富的 Markdown 切片，AI Agent 无需加载完整规范即可推理上下文、定位并调用目标接口。",
    cta: "获取 Markdown",
    href: "/apidocs.md",
    external: true,
    icon: (
      <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <path d="M12 2L9.5 8.5 2 12l7.5 3.5L12 22l2.5-6.5L22 12l-7.5-3.5Z" />
      </svg>
    ),
  },
  {
    hex: "#1AAB4E",
    bg: "rgba(26, 171, 78, 0.05)",
    title: "结构化 API",
    desc: "JSON 端点供 CLI 工具、CI 流水线和自动化脚本直接消费 — 零解析成本，拿到即可用。",
    cta: "查看 JSON",
    href: "/api/v1/services",
    external: true,
    icon: (
      <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="16 18 22 12 16 6" />
        <polyline points="8 6 2 12 8 18" />
      </svg>
    ),
  },
] as const;

export default async function HomePage() {
  const services = await getServices();

  return (
    <main className="min-h-[100dvh]" style={{ background: "linear-gradient(180deg, #f8f9fb 0%, #ffffff 40%, #fafafa 100%)" }}>
      {/* 顶栏 */}
      <header className="sticky top-0 z-10 flex h-12 items-center px-6 v-glass" style={{ borderRadius: 0, borderLeft: "none", borderRight: "none", borderTop: "none" }}>
        <span className="text-sm font-semibold tracking-tight text-v-black">
          APIPrism
        </span>
        <nav className="ml-auto flex items-center gap-1">
          <Link
            href="/docs"
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-v-gray-600 transition-colors hover:bg-v-gray-50/80 hover:text-v-black"
          >
            文档中心
          </Link>
          <a
            href="https://github.com/yipengfei329/apiprism"
            target="_blank"
            rel="noreferrer"
            className="rounded-lg p-2 text-v-gray-500 transition-colors hover:bg-v-gray-50/80 hover:text-v-black"
            aria-label="GitHub"
          >
            <svg className="h-4 w-4" viewBox="0 0 16 16" fill="currentColor">
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
            </svg>
          </a>
        </nav>
      </header>

      {/* ── Hero ── */}
      <section className="relative mx-auto max-w-[1200px] px-6 pt-16 pb-12 overflow-hidden">
        {/* 装饰：三色光晕，暗示棱镜折射 */}
        <div
          className="pointer-events-none absolute -top-20 right-[10%] h-[360px] w-[360px] rounded-full opacity-[0.06]"
          style={{ background: "radial-gradient(circle, #7B2FBE 0%, transparent 70%)" }}
        />
        <div
          className="pointer-events-none absolute top-[40%] right-[30%] h-[280px] w-[280px] rounded-full opacity-[0.05]"
          style={{ background: "radial-gradient(circle, #0063CC 0%, transparent 70%)" }}
        />
        <div
          className="pointer-events-none absolute -bottom-24 -left-16 h-[320px] w-[320px] rounded-full opacity-[0.05]"
          style={{ background: "radial-gradient(circle, #1AAB4E 0%, transparent 70%)" }}
        />

        <p className="mb-5 font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500 v-fade-in">
          APIPrism
        </p>
        <h1
          className="text-[clamp(2.4rem,5vw,3.2rem)] font-semibold leading-[1.15] text-v-black v-slide-up"
          style={{ letterSpacing: "-0.05em" }}
        >
          一份 API，折射完整光谱
        </h1>
        <p className="mt-6 text-[17px] leading-[1.85] text-v-gray-600 v-slide-up v-delay-1">
          一束光经过棱镜，折射出完整色谱。一份 API 经过 APIPrism，亦然。
        </p>
        <p
          className="mt-4 text-[15px] italic text-v-gray-400 v-slide-up v-delay-2"
          style={{ letterSpacing: "0.01em" }}
        >
          Every API deserves to be understood — by humans, by agents, by machines.
        </p>
        <div className="mt-7 flex flex-wrap items-center gap-3 v-slide-up v-delay-3">
          <Link
            href="/docs"
            className="inline-flex h-10 items-center gap-2 rounded-xl bg-v-black px-5 text-sm font-medium text-v-white transition-all duration-300 hover:bg-v-gray-600 hover:shadow-lg hover:shadow-black/10 active:scale-[0.98]"
          >
            开始探索
            <svg className="h-3.5 w-3.5" viewBox="0 0 14 14" fill="none">
              <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </Link>
          <a
            href="https://github.com/yipengfei329/apiprism"
            target="_blank"
            rel="noreferrer"
            className="inline-flex h-10 items-center gap-2 rounded-xl px-5 text-sm font-medium text-v-gray-600 transition-all duration-300 v-glass-subtle hover:text-v-black active:scale-[0.98]"
          >
            <svg className="h-4 w-4" viewBox="0 0 16 16" fill="currentColor">
              <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
            </svg>
            GitHub
          </a>
        </div>
      </section>

      {/* ── 分隔线 ── */}
      <div className="mx-auto max-w-[1200px] px-6">
        <div className="h-px v-shimmer rounded-full" />
      </div>

      {/* ── 已注册服务（核心内容，紧跟 Hero） ── */}
      <section className="mx-auto max-w-[1200px] px-6 py-10">
        {services.length === 0 ? (
          <div className="rounded-2xl px-8 py-12 v-glass">
            <p className="font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500">
              暂无服务
            </p>
            <h2
              className="mt-3 text-2xl font-semibold text-v-black"
              style={{ letterSpacing: "-0.04em" }}
            >
              等待第一束光
            </h2>
            <p className="mt-3 max-w-[56ch] leading-7 text-v-gray-600">
              在应用中引入 Spring Boot Starter，适配器会在启动时自动将
              <code className="rounded-lg bg-v-white/80 px-1.5 py-0.5 font-mono text-sm v-ring-light">/v3/api-docs</code>
              {" "}推送到 Center — 你的第一道光谱即刻生成。
            </p>
          </div>
        ) : (
          <>
            <p className="mb-8 font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500 v-fade-in">
              已注册服务 — {services.length}
            </p>
            <div className="grid gap-4 sm:grid-cols-2">
              {services.map((service, idx) => (
                <Link
                  key={`${service.name}-${service.environment}`}
                  href={`/docs/${encodeURIComponent(service.name)}/${encodeURIComponent(service.environment)}`}
                  className={`group flex flex-col rounded-2xl bg-white/80 p-6 transition-all duration-300 v-card-full v-card-full-hover backdrop-blur-sm v-slide-up ${idx === 0 ? "v-delay-1" : idx === 1 ? "v-delay-2" : idx === 2 ? "v-delay-3" : "v-delay-4"}`}
                >
                  <h2
                    className="text-2xl font-semibold text-v-black transition-colors"
                    style={{ letterSpacing: "-0.04em" }}
                  >
                    {service.name}
                  </h2>

                  <div className="mt-4 flex flex-wrap items-center gap-1.5">
                    <span className="rounded-full bg-[#E3EEFF] px-2.5 py-0.5 font-mono text-xs font-semibold text-v-link">
                      {service.environment}
                    </span>
                    {service.version && (
                      <span className="rounded-full bg-v-gray-50 px-2.5 py-0.5 font-mono text-xs font-medium text-v-gray-500 v-ring-light">
                        v{service.version}
                      </span>
                    )}
                    {service.updatedAt && (
                      <span className="ml-auto font-mono text-[11px] text-v-gray-400">
                        {formatDate(service.updatedAt)}
                      </span>
                    )}
                  </div>

                  {service.groups.length > 0 && (
                    <div className="mt-5 flex flex-wrap gap-1.5">
                      {service.groups.map((group) => (
                        <span
                          key={group.slug}
                          className="rounded-full bg-v-gray-50/80 px-2.5 py-0.5 text-xs text-v-gray-500 v-ring-light"
                        >
                          {group.name}
                        </span>
                      ))}
                    </div>
                  )}

                  <div className="mt-auto flex items-center gap-1 pt-6 text-sm font-medium text-v-gray-600 transition-colors group-hover:text-v-black">
                    查看文档
                    <svg
                      className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-1"
                      viewBox="0 0 14 14"
                      fill="none"
                    >
                      <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </div>
                </Link>
              ))}
            </div>
          </>
        )}
      </section>

      {/* ── 光谱分隔 — 三色渐变线 ── */}
      <div className="mx-auto max-w-[1200px] px-6">
        <div className="relative h-px">
          <div
            className="absolute inset-0 rounded-full"
            style={{
              background: "linear-gradient(90deg, #7B2FBE 0%, #0063CC 50%, #1AAB4E 100%)",
              opacity: 0.25,
            }}
          />
        </div>
      </div>

      {/* ── 光谱 — 三种形态 ── */}
      <section className="mx-auto max-w-[1200px] px-6 py-10">
        <p className="mb-3 font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500 v-fade-in">
          Spectrum
        </p>
        <h2
          className="mb-10 text-2xl font-semibold leading-[1.2] text-v-black v-slide-up"
          style={{ letterSpacing: "-0.04em" }}
        >
          同一份规范，三道光，各得其所
        </h2>

        <div className="grid gap-5 md:grid-cols-3">
          {spectrum.map((beam, idx) => (
            <div
              key={beam.title}
              className={`group relative flex flex-col overflow-hidden rounded-2xl bg-white/80 p-6 pb-5 transition-all duration-300 v-card-full v-card-full-hover backdrop-blur-sm v-slide-up v-delay-${idx + 1}`}
            >
              {/* 顶部光束 */}
              <div
                className="absolute inset-x-0 top-0 h-[2px] opacity-50 transition-opacity duration-300 group-hover:opacity-100"
                style={{ background: `linear-gradient(90deg, transparent 5%, ${beam.hex} 50%, transparent 95%)` }}
              />

              {/* 图标 + 标题 */}
              <div className="flex items-center gap-3">
                <div
                  className="flex h-9 w-9 items-center justify-center rounded-xl transition-colors duration-300"
                  style={{ background: beam.bg, color: beam.hex }}
                >
                  {beam.icon}
                </div>
                <h3
                  className="text-lg font-semibold text-v-black"
                  style={{ letterSpacing: "-0.03em" }}
                >
                  {beam.title}
                </h3>
              </div>

              {/* 描述 */}
              <p className="mt-4 flex-1 text-[15px] leading-[1.8] text-v-gray-600">
                {beam.desc}
              </p>

              {/* CTA */}
              {beam.external ? (
                <a
                  href={beam.href}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-5 inline-flex items-center gap-1 text-sm font-medium transition-colors duration-200"
                  style={{ color: beam.hex }}
                >
                  {beam.cta}
                  <svg className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-0.5" viewBox="0 0 14 14" fill="none">
                    <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </a>
              ) : (
                <Link
                  href={beam.href}
                  className="mt-5 inline-flex items-center gap-1 text-sm font-medium transition-colors duration-200"
                  style={{ color: beam.hex }}
                >
                  {beam.cta}
                  <svg className="h-3.5 w-3.5 transition-transform duration-300 group-hover:translate-x-0.5" viewBox="0 0 14 14" fill="none">
                    <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </Link>
              )}
            </div>
          ))}
        </div>
      </section>

      {/* ── 页脚 ── */}
      <footer className="px-6 py-8" style={{ borderTop: "1px solid rgba(60, 60, 67, 0.08)" }}>
        <div className="mx-auto flex max-w-[1200px] flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-mono text-xs text-v-gray-400">
            &copy; {new Date().getFullYear()} APIPrism &middot; Every API deserves to be understood
          </p>
          <nav className="flex items-center gap-4">
            <a
              href="https://apiprism.ai"
              target="_blank"
              rel="noreferrer"
              className="font-mono text-xs text-v-gray-400 transition-colors hover:text-v-gray-600"
            >
              apiprism.ai
            </a>
            <a
              href="https://github.com/yipengfei329/apiprism"
              target="_blank"
              rel="noreferrer"
              className="font-mono text-xs text-v-gray-400 transition-colors hover:text-v-gray-600"
            >
              GitHub
            </a>
            <Link
              href="/docs"
              className="font-mono text-xs text-v-gray-400 transition-colors hover:text-v-gray-600"
            >
              文档中心
            </Link>
          </nav>
        </div>
      </footer>
    </main>
  );
}
