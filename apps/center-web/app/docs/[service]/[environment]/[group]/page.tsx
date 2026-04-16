import Link from "next/link";
import { notFound } from "next/navigation";
import { getGroup, getMcpGroupStatus } from "../../../lib/api";
import { HtmlText } from "../../../components/HtmlText";
import { MethodBadge } from "../../../components/MethodBadge";
import { Breadcrumb } from "../../../components/Breadcrumb";
import { AgentDocLink } from "../../../components/AgentDocLink";
import { McpToggle } from "../../../components/McpToggle";


type Props = {
  params: Promise<{ service: string; environment: string; group: string }>;
};

export default async function GroupPage({ params }: Props) {
  const { service, environment, group } = await params;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);
  const grpSlug = decodeURIComponent(group);

  const [data, mcpStatus] = await Promise.all([
    getGroup(svc, env, grpSlug),
    getMcpGroupStatus(svc, env, grpSlug),
  ]);
  if (!data) notFound();

  return (
    <div>
      {/* 面包屑导航：sticky，毛玻璃 */}
      <div className="sticky top-0 z-10 px-4 py-3 sm:px-8 v-glass" style={{ borderRadius: 0, borderLeft: "none", borderRight: "none", borderTop: "none" }}>
        <div className="mx-auto max-w-[1100px]">
          <Breadcrumb
            items={[
              {
                label: svc,
                href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}`,
                icon: "service",
              },
              {
                label: data.name,
                icon: "group",
              },
            ]}
          />
        </div>
      </div>

      {/* 分组头部区域 */}
      <div style={{ background: "linear-gradient(180deg, rgba(242,242,247,0.6) 0%, rgba(242,242,247,0.2) 100%)" }}>
        <div className="mx-auto max-w-[1100px] px-4 pb-8 pt-8 sm:px-8 sm:pt-14">
          <div className="flex items-start justify-between gap-4">
            <h1
              className="text-[clamp(1.6rem,3vw,2.2rem)] font-semibold leading-tight text-v-black"
              style={{ letterSpacing: "-0.025em" }}
            >
              {data.name}
            </h1>
            <AgentDocLink path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/apidocs.md`} />
          </div>
          {data.description && (
            <HtmlText
              as="div"
              text={data.description}
              className="mt-3 max-w-[65ch] text-[15px] leading-[1.8] text-v-gray-500 [&>p]:mt-2 [&>p:first-child]:mt-0 [&_ul]:mt-1.5 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mt-1.5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mt-0.5 [&_code]:rounded [&_code]:bg-v-gray-50 [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[13px]"
            />
          )}
          <div className="mt-6 font-mono text-[13px] font-medium text-v-gray-400">
            {data.operations.length} 个接口
          </div>
        </div>
      </div>

      {/* MCP 分组开关 */}
      <div className="mx-auto max-w-[1100px] px-4 pt-6 sm:px-8 sm:pt-8">
        <McpToggle
          service={svc}
          environment={env}
          groupSlug={grpSlug}
          initialEnabled={mcpStatus?.enabled ?? false}
          initialSseEndpoint={mcpStatus?.sseEndpoint ?? null}
          initialStreamableEndpoint={mcpStatus?.streamableEndpoint ?? null}
        />
      </div>

      {/* 接口列表 */}
      <div className="mx-auto max-w-[1100px] px-4 py-8 sm:px-8 sm:py-12">
        <section>
          <p className="mb-6 text-[12px] font-semibold uppercase tracking-[0.08em] text-v-gray-400">
            接口列表
          </p>

          {data.operations.length === 0 ? (
            <p className="text-v-gray-400">该分组下暂无接口。</p>
          ) : (
            <div className="space-y-2">
              {data.operations.map((op) => (
                <Link
                  key={op.operationId}
                  href={`/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}/${encodeURIComponent(op.operationId)}`}
                  className="group flex items-start gap-4 rounded-xl bg-white/90 px-5 py-4 backdrop-blur-sm transition-all v-card-full v-card-full-hover"
                >
                  <div className="shrink-0 pt-0.5">
                    <MethodBadge method={op.method} size="md" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <code className="font-mono text-[12px] text-v-gray-400">{op.path}</code>
                    <p className="mt-0.5 text-[14px] font-medium text-v-black transition-colors group-hover:text-v-link">
                      {op.summary || op.operationId}
                    </p>
                    {op.description && (
                      <HtmlText
                        as="p"
                        text={op.description}
                        className="mt-1 line-clamp-1 text-[13px] leading-[1.5] text-v-gray-500"
                      />
                    )}
                  </div>
                  <svg
                    className="mt-1 h-3.5 w-3.5 shrink-0 text-v-gray-400 opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:opacity-100"
                    viewBox="0 0 14 14"
                    fill="none"
                  >
                    <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                  </svg>
                </Link>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
