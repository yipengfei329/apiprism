import Link from "next/link";
import { getInternalApiUrl } from "@/app/lib/internal-api";
import { ParticleField } from "./components/ParticleField";

type GroupRef = { name: string; slug: string };

type ServiceCatalogItem = {
  name: string;
  environment: string;
  title: string;
  version: string;
  updatedAt: string;
  groups: GroupRef[];
  /** 接口（operation）总数。后端字段 operationCount。 */
  operationCount: number;
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

function fmtTime(iso: string): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return "—";
  return d.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
}

/** 环境配色：常暗，与主题切换解耦 */
const ENV_TONES: Record<string, { dot: string; ring: string }> = {
  prod: { dot: "#2DD4BF", ring: "rgba(45, 212, 191, 0.35)" },
  production: { dot: "#2DD4BF", ring: "rgba(45, 212, 191, 0.35)" },
  staging: { dot: "#FBBF24", ring: "rgba(251, 191, 36, 0.35)" },
  test: { dot: "#A78BFA", ring: "rgba(167, 139, 250, 0.35)" },
};

function envTone(env: string) {
  return ENV_TONES[env.toLowerCase()] ?? { dot: "#94A3B8", ring: "rgba(148, 163, 184, 0.32)" };
}

export default async function HomePage() {
  const services = await getServices();
  const envCount = new Set(services.map((s) => s.environment)).size;
  const operationTotal = services.reduce<number>((acc, s) => acc + (s.operationCount ?? 0), 0);
  const lastSync = services.reduce<string>(
    (acc, s) => (s.updatedAt > acc ? s.updatedAt : acc),
    "",
  );
  const padded = (n: number) => String(n).padStart(2, "0");

  return (
    <main className="relative min-h-[100dvh] overflow-hidden bg-[#04050a] text-white">
      {/* ─────────── 全屏粒子背景（fixed，跟随视口） ─────────── */}
      <div className="pointer-events-none fixed inset-0 z-0">
        <ParticleField density={0.0001} maxLink={160} maxPulses={8} />
      </div>
      <div className="pointer-events-none fixed inset-0 z-0 hero-radial-glow" />
      <div className="pointer-events-none fixed inset-0 z-0 hero-grid opacity-[0.32]" />
      <div className="pointer-events-none fixed inset-0 z-0 hero-vignette" />

      {/* ─────────── 顶栏：极简 HUD ─────────── */}
      <header className="relative z-30 border-b border-white/[0.05] bg-black/30 backdrop-blur-xl">
        <div className="mx-auto flex h-11 max-w-[1280px] items-center px-6">
          <Link href="/" className="flex items-center gap-2">
            <span className="relative inline-flex h-4 w-4 items-center justify-center">
              <span className="absolute inset-0 rounded-full bg-gradient-to-tr from-teal-400 via-cyan-300 to-violet-400 opacity-80 blur-[5px]" />
              <span className="relative h-1.5 w-1.5 rounded-full bg-white" />
            </span>
            <span className="font-mono text-[11px] font-semibold uppercase tracking-[0.18em] text-white">
              APIPrism
            </span>
          </Link>
          <nav className="ml-auto flex items-center gap-1">
            <Link
              href="/docs"
              className="rounded-md px-2.5 py-1 text-[12px] font-medium tracking-wide text-white/65 transition-colors hover:bg-white/[0.06] hover:text-white"
            >
              文档
            </Link>
            <a
              href="https://github.com/yipengfei329/apiprism"
              target="_blank"
              rel="noreferrer"
              className="rounded-md p-1.5 text-white/55 transition-colors hover:bg-white/[0.06] hover:text-white"
              aria-label="GitHub"
            >
              <svg className="h-3.5 w-3.5" viewBox="0 0 16 16" fill="currentColor">
                <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27s1.36.09 2 .27c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
              </svg>
            </a>
          </nav>
        </div>
      </header>

      {/* ─────────── HUD 主区：状态行 + 大计数 + 服务网格 ─────────── */}
      <section className="relative z-20 mx-auto max-w-[1280px] px-6 pb-40 pt-20">
        {/* 顶部数据条 */}
        <div className="hero-chip-fade-in flex items-center gap-3 text-[11px] tracking-wide text-white/55">
          <span className="relative flex h-1.5 w-1.5">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-teal-400 opacity-70" />
            <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-teal-300" />
          </span>
          <span className="text-teal-300/90">在线</span>
          <span className="text-white/25">·</span>
          <span>
            最近同步 <span className="font-mono">{fmtTime(lastSync)}</span>
          </span>
        </div>

        {/* HUD 大字 */}
        <h1 className="hero-title-fade-in mt-6 text-[clamp(2.4rem,5.6vw,4.4rem)] font-semibold leading-[1.05] tracking-[-0.02em]">
          <span className="hero-gradient-text-1 font-mono">{padded(services.length)}</span>
          <span className="text-white/35"> 个服务</span>
          <span className="mx-1 text-white/20">/</span>
          <span className="hero-gradient-text-2 font-mono">{padded(envCount)}</span>
          <span className="text-white/35"> 个环境</span>
          <span className="mx-1 text-white/20">/</span>
          <span className="hero-gradient-text-1 font-mono">{padded(operationTotal)}</span>
          <span className="text-white/35"> 个接口</span>
        </h1>

        {/* ═════ 服务注册表（行式，无卡片）/ 空态 ═════ */}
        <div className="hero-cta-fade-in mt-14">
          {/* registry 标头 */}
          <div className="flex items-center gap-4 text-[11px] tracking-wide text-white/45">
            <span className="font-mono text-[10px] uppercase tracking-[0.32em] text-teal-300/70">
              {"// 服务注册表"}
            </span>
            <span className="h-px flex-1 bg-gradient-to-r from-white/20 to-transparent" />
            <span className="hidden sm:inline">⌄ 滚动浏览</span>
          </div>

          {services.length === 0 ? (
            <div className="mt-8">
              <EmptyConsole />
            </div>
          ) : (
            <ul className="mt-2 border-t border-white/[0.06]">
              {services.map((service, idx) => {
                const tone = envTone(service.environment);
                return (
                  <li
                    key={`${service.name}-${service.environment}`}
                    className="border-b border-white/[0.06]"
                  >
                    <Link
                      href={`/docs/${encodeURIComponent(service.name)}/${encodeURIComponent(service.environment)}`}
                      className="registry-row group relative block py-7 pl-4 pr-2 transition-colors duration-200 hover:bg-white/[0.025] sm:pl-6"
                      style={
                        {
                          animationDelay: `${0.85 + idx * 0.05}s`,
                          ["--tone-dot" as string]: tone.dot,
                          ["--tone-ring" as string]: tone.ring,
                        } as React.CSSProperties
                      }
                    >
                      {/* 左轨：hover 时点亮 */}
                      <span className="registry-rail" aria-hidden />

                      {/* 单行栅格 */}
                      <div className="grid grid-cols-[2.5rem_1fr_auto] items-center gap-4 sm:grid-cols-[3rem_1fr_auto] sm:gap-8">
                        {/* index */}
                        <span className="font-mono text-[11px] tracking-[0.22em] text-white/30 transition-colors duration-200 group-hover:text-teal-300/80">
                          {padded(idx + 1)}
                        </span>

                        {/* 名字 + 元信息 */}
                        <div className="min-w-0">
                          <h3 className="registry-name truncate font-mono font-semibold tracking-tight text-white text-[clamp(1.4rem,2.6vw,2.1rem)] leading-[1.1]">
                            {service.name}
                          </h3>
                          <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-[12px] tracking-wide text-white/55">
                            <span className="inline-flex items-center gap-1.5">
                              <span
                                className="inline-flex h-1.5 w-1.5 rounded-full"
                                style={{
                                  background: tone.dot,
                                  boxShadow: `0 0 8px ${tone.ring}`,
                                }}
                              />
                              <span className="font-mono uppercase tracking-[0.18em]" style={{ color: tone.dot }}>
                                {service.environment}
                              </span>
                            </span>
                            {service.version && (
                              <>
                                <span className="text-white/15">·</span>
                                <span className="font-mono">v{service.version}</span>
                              </>
                            )}
                            <span className="text-white/15">·</span>
                            <span>
                              <span className="font-mono text-white/85">{service.operationCount ?? 0}</span>{" "}
                              <span className="text-white/45">个接口</span>
                            </span>
                            {service.groups.length > 0 && (
                              <>
                                <span className="text-white/15">·</span>
                                <span>
                                  <span className="font-mono text-white/85">{service.groups.length}</span>{" "}
                                  <span className="text-white/45">个分组</span>
                                </span>
                              </>
                            )}
                            <span className="text-white/15">·</span>
                            <span className="text-white/45">
                              同步 <span className="font-mono text-white/65">{fmtTime(service.updatedAt)}</span>
                            </span>
                          </div>
                        </div>

                        {/* 进入 → */}
                        <span className="hidden items-center gap-2 text-[12px] tracking-wide text-white/50 transition-colors duration-200 group-hover:text-teal-300 sm:inline-flex">
                          进入
                          <svg
                            className="h-3 w-3 transition-transform duration-200 group-hover:translate-x-1.5"
                            viewBox="0 0 14 14"
                            fill="none"
                          >
                            <path
                              d="M3 7h8M7.5 3.5L11 7l-3.5 3.5"
                              stroke="currentColor"
                              strokeWidth="1.8"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                          </svg>
                        </span>
                      </div>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </section>

      {/* ─────────── 底部 HUD 状态栏 ─────────── */}
      <footer className="fixed inset-x-0 bottom-0 z-30 border-t border-white/[0.05] bg-black/40 backdrop-blur-xl">
        <div className="mx-auto flex h-7 max-w-[1280px] items-center gap-4 px-6 text-[11px] tracking-wide text-white/50">
          <span className="text-teal-300/85">● 在线</span>
          <span className="text-white/15">/</span>
          <span>APIPrism · 服务图谱</span>
          <span className="ml-auto hidden sm:inline">每一个 API，都值得被充分理解</span>
        </div>
      </footer>
    </main>
  );
}

/* ═════════════════════ 空态：终端风格 ═════════════════════ */
function EmptyConsole() {
  return (
    <div className="hud-tile relative overflow-hidden rounded-xl border border-white/[0.08] bg-white/[0.02] px-6 py-8 backdrop-blur-md sm:px-8 sm:py-10">
      <span className="hud-corner hud-corner-tl" />
      <span className="hud-corner hud-corner-tr" />
      <span className="hud-corner hud-corner-bl" />
      <span className="hud-corner hud-corner-br" />
      <pre className="relative font-mono text-[12px] leading-[1.9] text-white/65 sm:text-[13px]">
        <span className="text-teal-300/85">$</span> apiprism atlas init
        {"\n"}
        <span className="text-white/40">{"// 等待首次服务接入…"}</span>
        {"\n\n"}
        <span className="text-white/40">{"// 提示："}</span>{" "}
        <span className="text-white/85">implementation(&quot;ai.apiprism:apiprism-spring-boot-starter&quot;)</span>
        <span className="hud-cursor ml-1 inline-block h-3 w-2 translate-y-[2px] bg-teal-300" />
      </pre>
    </div>
  );
}
