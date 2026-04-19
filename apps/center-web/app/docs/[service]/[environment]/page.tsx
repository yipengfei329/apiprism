import Link from "next/link";
import { notFound } from "next/navigation";
import { getServiceSnapshot, getMcpServiceStatus } from "../../lib/api";
import { Breadcrumb } from "../../components/Breadcrumb";
import { HtmlText } from "../../components/HtmlText";
import { McpToggle } from "../../components/McpToggle";


type Props = {
  params: Promise<{ service: string; environment: string }>;
};

export default async function ServiceOverviewPage({ params }: Props) {
  const { service, environment } = await params;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);

  const [snapshot, mcpStatus] = await Promise.all([
    getServiceSnapshot(svc, env),
    getMcpServiceStatus(svc, env),
  ]);
  if (!snapshot) notFound();

  return (
    <div className="mx-auto max-w-[820px] px-4 py-8 sm:px-8 sm:py-14">
      {/* 面包屑 */}
      <div className="mb-8">
        <Breadcrumb
          items={[
            {
              label: svc,
              icon: "service",
            },
          ]}
        />
      </div>

      {/* 服务头部 */}
      <header className="mb-14">
        <h1
          className="text-[clamp(1.8rem,3vw,2.6rem)] font-semibold leading-tight text-[#1C1C1E]"
          style={{ letterSpacing: "-0.035em" }}
        >
          {svc}
        </h1>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          {snapshot.version && (
            <span className="rounded-md bg-v-gray-50 px-2.5 py-0.5 font-mono text-[12px] font-semibold text-v-gray-500 v-ring-light">
              v{snapshot.version}
            </span>
          )}
          <span className="rounded-md bg-[#F4F4F5] px-2.5 py-0.5 font-mono text-[12px] font-medium text-[#636366]">
            {env}
          </span>
          <a
            href={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/apidocs.md`}
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1 rounded-md px-2.5 py-0.5 font-mono text-[12px] font-medium text-v-gray-400 transition-colors hover:text-v-black hover:bg-v-gray-50"
          >
            <svg className="h-3 w-3" viewBox="0 0 256 256" fill="currentColor">
              <path d="M200,48H136V16a8,8,0,0,0-16,0V48H56A32,32,0,0,0,24,80V192a32,32,0,0,0,32,32H200a32,32,0,0,0,32-32V80A32,32,0,0,0,200,48Zm16,144a16,16,0,0,1-16,16H56a16,16,0,0,1-16-16V80A16,16,0,0,1,56,64H200a16,16,0,0,1,16,16Zm-36-80a12,12,0,1,1-12-12A12,12,0,0,1,180,112Zm-44,0a12,12,0,1,1-12-12A12,12,0,0,1,136,112Zm-44,0a12,12,0,1,1-12-12A12,12,0,0,1,92,112Zm88,40H84a8,8,0,0,0-6.26,13l28,36a8,8,0,0,0,12.52,0L128,188.94,137.74,201a8,8,0,0,0,12.52,0l28-36A8,8,0,0,0,172,152Z" />
            </svg>
            Agent Docs
          </a>
        </div>

        {snapshot.serverUrls?.length > 0 && (
          <div className="mt-8">
            <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.08em] text-[#8E8E93]">
              服务地址
            </p>
            <div className="flex flex-wrap gap-2">
              {snapshot.serverUrls.map((url) => (
                <code
                  key={url}
                  className="rounded-lg bg-[#F4F4F5] px-3 py-1.5 font-mono text-[13px] text-[#1C1C1E]"
                >
                  {url}
                </code>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* MCP 服务开关 */}
      <section className="mb-14">
        <p className="mb-4 text-[12px] font-semibold uppercase tracking-[0.08em] text-[#8E8E93]">
          MCP 网关
        </p>
        <McpToggle
          service={svc}
          environment={env}
          initialEnabled={mcpStatus?.serviceEnabled ?? false}
          initialSseEndpoint={mcpStatus?.sseEndpoint ?? null}
          initialStreamableEndpoint={mcpStatus?.streamableEndpoint ?? null}
        />
      </section>

      {/* 分组卡片网格 */}
      <section>
        <p className="mb-6 text-[12px] font-semibold uppercase tracking-[0.08em] text-[#8E8E93]">
          接口分组 — {snapshot.groups.length}
        </p>

        {snapshot.groups.length === 0 ? (
          <p className="text-[#8E8E93]">该服务下暂无接口分组。</p>
        ) : (
          <div className="grid gap-3 sm:grid-cols-2">
            {snapshot.groups.map((group) => (
              <Link
                key={group.name}
                href={`/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(group.slug)}`}
                className="group rounded-xl bg-white p-5 transition-all v-card-full v-card-full-hover"
              >
                <h3
                  className="font-semibold text-[#1C1C1E]"
                  style={{ letterSpacing: "-0.015em" }}
                >
                  {group.name}
                </h3>
                {group.description && (
                  <HtmlText
                    as="p"
                    text={group.description}
                    className="mt-1.5 line-clamp-2 text-[13px] leading-[1.65] text-[#636366]"
                  />
                )}
                <div className="mt-4 flex items-center justify-between">
                  <p className="font-mono text-[12px] text-[#8E8E93]">
                    {group.operations.length} 个接口
                  </p>
                  <svg
                    className="h-3.5 w-3.5 text-[#8E8E93] opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:opacity-100"
                    viewBox="0 0 14 14"
                    fill="none"
                  >
                    <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
