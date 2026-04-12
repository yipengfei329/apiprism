import Link from "next/link";

type ServiceCatalogItem = {
  name: string;
  environment: string;
  title: string;
  version: string;
  updatedAt: string;
  groups: string[];
};

const apiBase = process.env.NEXT_PUBLIC_APIPRISM_API_BASE ?? "http://localhost:8080";

async function getServices(): Promise<ServiceCatalogItem[]> {
  try {
    const response = await fetch(`${apiBase}/api/v1/services`, { cache: "no-store" });
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

export default async function HomePage() {
  const services = await getServices();

  return (
    <main className="min-h-[100dvh] bg-v-white">
      {/* 顶栏 */}
      <header className="sticky top-0 z-10 flex h-12 items-center border-b border-v-gray-100 bg-v-white/90 px-6 backdrop-blur-md">
        <span className="text-sm font-semibold tracking-tight text-v-black">
          APIPrism
        </span>
        <nav className="ml-auto flex items-center gap-1">
          <Link
            href="/docs"
            className="rounded-lg px-3 py-1.5 text-sm font-medium text-v-gray-600 transition-colors hover:bg-v-gray-50 hover:text-v-black"
          >
            文档中心
          </Link>
        </nav>
      </header>

      {/* Hero */}
      <section className="mx-auto max-w-[1200px] px-6 pt-20 pb-16">
        <p className="mb-5 font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500">
          APIPrism Center
        </p>
        <h1
          className="max-w-[20ch] text-[clamp(2.4rem,5vw,3rem)] font-semibold leading-[1.1] text-v-black"
          style={{ letterSpacing: "-0.05em" }}
        >
          OpenAPI 规范，为人与智能体重新构建
        </h1>
        <p className="mt-6 max-w-[52ch] text-lg font-normal leading-[1.9] text-v-gray-600">
          Center 从各语言适配器接收 OpenAPI 快照，提供规范化的服务目录、分组文档浏览，以及面向智能体的 Markdown 视图。
        </p>
        <div className="mt-8 flex flex-wrap items-center gap-3">
          <Link
            href="/docs"
            className="inline-flex h-9 items-center gap-2 rounded-xl bg-v-black px-4 text-sm font-medium text-v-white transition-colors hover:bg-v-gray-600"
          >
            浏览文档
            <svg className="h-3.5 w-3.5" viewBox="0 0 14 14" fill="none">
              <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </Link>
          <a
            href={`${apiBase}/api/v1/services`}
            target="_blank"
            rel="noreferrer"
            className="inline-flex h-9 items-center gap-2 rounded-xl px-4 text-sm font-medium text-v-gray-600 transition-colors v-ring hover:bg-v-gray-50 hover:text-v-black"
          >
            API JSON
          </a>
        </div>
      </section>

      {/* 分隔线 */}
      <div className="border-t border-v-gray-100" />

      {/* 服务列表 */}
      <section className="mx-auto max-w-[1200px] px-6 py-16">
        {services.length === 0 ? (
          <div className="rounded-2xl bg-v-gray-50 px-8 py-12 v-ring">
            <p className="font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500">
              暂无服务
            </p>
            <h2
              className="mt-3 text-2xl font-semibold text-v-black"
              style={{ letterSpacing: "-0.04em" }}
            >
              尚未注册任何服务
            </h2>
            <p className="mt-3 max-w-[56ch] leading-7 text-v-gray-600">
              启动 Center 服务端，在应用中引入 Spring Boot Starter，适配器将自动注册
              <code className="rounded-lg bg-v-white px-1.5 py-0.5 font-mono text-sm v-ring-light">/v3/api-docs</code> 的输出。
            </p>
          </div>
        ) : (
          <>
            <p className="mb-8 font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500">
              已注册服务 — {services.length}
            </p>
            <div className="grid gap-4 sm:grid-cols-2">
              {services.map((service) => (
                <Link
                  key={`${service.name}-${service.environment}`}
                  href={`/docs/${encodeURIComponent(service.name)}/${encodeURIComponent(service.environment)}`}
                  className="group flex flex-col rounded-xl border border-v-border bg-v-white p-6 transition-all v-card-full-hover"
                >
                  {/* 服务名称 */}
                  <h2
                    className="text-2xl font-semibold text-v-black transition-colors"
                    style={{ letterSpacing: "-0.04em" }}
                  >
                    {service.name}
                  </h2>

                  {/* 环境 + 版本徽章 */}
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

                  {/* 接口分组 */}
                  {service.groups.length > 0 && (
                    <div className="mt-5 flex flex-wrap gap-1.5">
                      {service.groups.map((group) => (
                        <span
                          key={group}
                          className="rounded-full bg-v-gray-50 px-2.5 py-0.5 text-xs text-v-gray-500 v-ring-light"
                        >
                          {group}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* 底部 CTA */}
                  <div className="mt-auto flex items-center gap-1 pt-6 text-sm font-medium text-v-gray-600 transition-colors group-hover:text-v-black">
                    查看文档
                    <svg
                      className="h-3.5 w-3.5 transition-transform group-hover:translate-x-0.5"
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

      {/* 分隔线 */}
      <div className="border-t border-v-gray-100" />

      {/* 工作原理 */}
      <section className="mx-auto max-w-[1200px] px-6 py-16">
        <p className="mb-8 font-mono text-xs font-medium uppercase tracking-[0.2em] text-v-gray-500">
          工作原理
        </p>
        <div className="grid gap-4 md:grid-cols-3">
          {[
            {
              step: "01",
              hex: "#0063CC",
              title: "适配",
              body: "语言适配器捕获服务的 OpenAPI 规范与元数据，在启动时将注册载荷推送到 Center。",
            },
            {
              step: "02",
              hex: "#C0148A",
              title: "规范化",
              body: "Center 将原始 OpenAPI 解析并规范化为稳定的标准模型 — 服务、分组、接口 — 跨所有适配器保持一致。",
            },
            {
              step: "03",
              hex: "#E0341A",
              title: "探索",
              body: "在可读性良好的界面中浏览分组目录，或获取针对性的 Markdown 切片供智能体消费，无需加载完整规范。",
            },
          ].map(({ step, hex, title, body }) => (
            <div key={step} className="rounded-xl border border-v-border bg-v-white p-6">
              <p className="font-mono text-xs font-medium" style={{ color: hex }}>
                {step}
              </p>
              <h3
                className="mt-3 text-2xl font-semibold text-v-black"
                style={{ letterSpacing: "-0.04em" }}
              >
                {title}
              </h3>
              <p className="mt-3 leading-7 text-v-gray-600">{body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* 页脚 */}
      <footer className="border-t border-v-gray-100 px-6 py-8">
        <p className="mx-auto max-w-[1200px] font-mono text-xs text-v-gray-400">
          APIPrism Center — 面向人类与智能体的 OpenAPI 目录
        </p>
      </footer>
    </main>
  );
}
