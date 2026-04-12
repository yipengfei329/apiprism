import Link from "next/link";
import { notFound } from "next/navigation";
import { getGroup } from "../../../../lib/api";
import { MethodBadge } from "../../../../components/MethodBadge";
import { Breadcrumb, type BreadcrumbItem } from "../../../../components/Breadcrumb";


type Props = {
  params: Promise<{ service: string; environment: string; group: string }>;
};

export default async function GroupPage({ params }: Props) {
  const { service, environment, group } = await params;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);
  const grp = decodeURIComponent(group);

  const data = await getGroup(svc, env, grp);
  if (!data) notFound();

  return (
    <div className="mx-auto max-w-[820px] px-8 py-14">
      {/* 面包屑 */}
      <div className="mb-8">
        <Breadcrumb
          items={[
            {
              label: svc,
              href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}`,
              icon: "service",
            },
            {
              label: grp,
              icon: "group",
            },
          ]}
        />
      </div>

      {/* 分组标题 */}
      <header className="mb-14">
        <h1
          className="text-[clamp(1.6rem,3vw,2.2rem)] font-semibold leading-tight text-[#1C1C1E]"
          style={{ letterSpacing: "-0.025em" }}
        >
          {data.name}
        </h1>
        {data.description && (
          <p className="mt-3 max-w-[65ch] text-[15px] leading-[1.8] text-[#636366]">
            {data.description}
          </p>
        )}
      </header>

      {/* 接口列表 */}
      <section>
        <div className="mb-6 flex items-center gap-2.5">
          <p className="text-[12px] font-semibold uppercase tracking-[0.08em] text-[#8E8E93]">
            接口列表
          </p>
          <span className="rounded-md bg-[#F4F4F5] px-2 py-0.5 font-mono text-[11px] font-semibold text-[#8E8E93]">
            {data.operations.length}
          </span>
        </div>

        {data.operations.length === 0 ? (
          <p className="text-[#8E8E93]">该分组下暂无接口。</p>
        ) : (
          <div className="space-y-2">
            {data.operations.map((op) => (
              <Link
                key={op.operationId}
                href={`/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/ops/${encodeURIComponent(op.operationId)}`}
                className="group flex items-start gap-4 rounded-xl border border-[#E8E8EC] bg-white px-5 py-4 transition-all v-card-full-hover"
              >
                <div className="shrink-0 pt-0.5">
                  <MethodBadge method={op.method} size="md" />
                </div>
                <div className="min-w-0 flex-1">
                  <code className="font-mono text-[12px] text-[#8E8E93]">{op.path}</code>
                  <p className="mt-0.5 text-[14px] font-medium text-[#1C1C1E] transition-colors group-hover:text-[#0063CC]">
                    {op.summary || op.operationId}
                  </p>
                  {op.description && (
                    <p className="mt-1 line-clamp-1 text-[13px] leading-[1.5] text-[#636366]">
                      {op.description}
                    </p>
                  )}
                </div>
                <svg
                  className="mt-1 h-3.5 w-3.5 shrink-0 text-[#8E8E93] opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:opacity-100"
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
  );
}
