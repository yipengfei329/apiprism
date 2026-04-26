import Link from "next/link";
import { notFound } from "next/navigation";
import {
  getGroup,
  getMcpGroupStatus,
  getRevisionGroup,
  listRevisions,
} from "../../../lib/api";
import { HtmlText } from "../../../components/HtmlText";
import { MethodBadge } from "../../../components/MethodBadge";
import { Breadcrumb } from "../../../components/Breadcrumb";
import { AgentDocLink } from "../../../components/AgentDocLink";
import { DocsHeaderSearch } from "../../../components/DocsHeaderSearch";
import { McpToggle } from "../../../components/McpToggle";
import { RevisionBanner } from "../../../components/RevisionBanner";
import { SectionLabel } from "../../../components/SectionLabel";


type Props = {
  params: Promise<{ service: string; environment: string; group: string }>;
  searchParams: Promise<{ revision?: string }>;
};

const METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

function GroupKicker({ service, env }: { service: string; env: string }) {
  return (
    <div className="docs-route-kicker mb-5 inline-flex max-w-full items-center gap-2.5 font-mono text-[11.5px]">
      <span className="rounded-md border px-2 py-1 font-semibold uppercase tracking-[0.12em] text-[var(--accent)]">
        group
      </span>
      <span className="truncate text-[var(--text-tertiary)]">{service}</span>
      <span className="text-[var(--text-quaternary)]">/</span>
      <span className="font-medium uppercase tracking-wider text-[var(--text-secondary)]">{env}</span>
    </div>
  );
}

export default async function GroupPage({ params, searchParams }: Props) {
  const { service, environment, group } = await params;
  const { revision: revisionParam } = await searchParams;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);
  const grpSlug = decodeURIComponent(group);

  const [data, mcpStatus, revisions] = await Promise.all([
    revisionParam
      ? getRevisionGroup(svc, env, revisionParam, grpSlug)
      : getGroup(svc, env, grpSlug),
    revisionParam ? Promise.resolve(null) : getMcpGroupStatus(svc, env, grpSlug),
    revisionParam ? listRevisions(svc, env) : Promise.resolve([]),
  ]);
  if (!data) notFound();

  const viewingRevision = revisionParam
    ? revisions.find((r) => r.id === revisionParam) ?? null
    : null;
  const viewingOlder = Boolean(viewingRevision && !viewingRevision.current);
  const querySuffix = revisionParam
    ? `?revision=${encodeURIComponent(revisionParam)}`
    : "";
  const methodCounts: Record<string, number> = {};
  for (const op of data.operations) {
    const method = op.method.toUpperCase();
    methodCounts[method] = (methodCounts[method] ?? 0) + 1;
  }
  const methods = [
    ...METHOD_ORDER.filter((method) => methodCounts[method]).map((method) => ({
      method,
      count: methodCounts[method],
    })),
    ...Object.entries(methodCounts)
      .filter(([method]) => !METHOD_ORDER.includes(method))
      .sort((a, b) => b[1] - a[1])
      .map(([method, count]) => ({ method, count })),
  ];

  return (
    <div>
      {/* 面包屑 sticky */}
      <div className="docs-sticky-bar sticky top-0 z-30 min-h-[var(--docs-header-height)] px-6 py-3 sm:px-10">
        <div className="mx-auto flex max-w-[1100px] items-center justify-between gap-3">
          <div className="min-w-0">
            <Breadcrumb
              items={[
                {
                  label: svc,
                  href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}${querySuffix}`,
                  icon: "service",
                },
                { label: data.name, icon: "group" },
              ]}
            />
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <DocsHeaderSearch service={svc} environment={env} revision={revisionParam ?? null} />
            <AgentDocLink
              path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}/apidocs.md`}
            />
          </div>
        </div>
      </div>

      {viewingOlder && viewingRevision && (
        <div className="mx-auto max-w-[1100px] px-6 pt-6 sm:px-10">
          <RevisionBanner
            service={svc}
            environment={env}
            revisionId={viewingRevision.id}
            seq={viewingRevision.seq}
            registeredAt={viewingRevision.registeredAt}
          />
        </div>
      )}

      <div className="docs-reference-hero">
        <div className="mx-auto max-w-[1100px] px-6 pb-8 pt-12 sm:px-10 sm:pt-16">
          <GroupKicker service={svc} env={env} />

          <header>
            <div className="max-w-[820px]">
              <h1 className="text-[clamp(1.75rem,3vw,2.35rem)] font-semibold leading-[1.08] tracking-tight text-[var(--text-primary)]">
              {data.name}
              </h1>
              {data.description && (
                <HtmlText
                  as="div"
                  text={data.description}
                  className="mt-5 max-w-[70ch] text-[15px] leading-[1.78] text-[var(--text-secondary)] [&>p]:mt-2.5 [&>p:first-child]:mt-0 [&_ul]:mt-1.5 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mt-1.5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mt-0.5 [&_code]:rounded [&_code]:bg-[var(--bg-subtle)] [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[13px]"
                />
              )}
            </div>

            <div className="docs-facts-strip mt-7">
              <dl className="grid grid-cols-2 gap-0 md:grid-cols-[1fr_1fr_2fr]">
                <div className="min-w-0 px-4 py-3 first:pl-0 md:border-l md:border-[var(--border-subtle)] md:first:border-l-0 md:first:pl-0">
                  <dt className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)]">接口</dt>
                  <dd className="mt-1.5 text-[13px] font-semibold leading-snug tabular-nums text-[var(--text-secondary)]">{data.operations.length} 个</dd>
                </div>
                <div className="min-w-0 px-4 py-3 md:border-l md:border-[var(--border-subtle)]">
                  <dt className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)]">MCP</dt>
                  <dd className="mt-1.5 text-[13px] font-medium leading-snug text-[var(--text-secondary)]">
                    {viewingOlder ? "历史版本" : mcpStatus?.enabled ? "已启用" : "未启用"}
                  </dd>
                </div>
                <div className="min-w-0 px-4 py-3 md:border-l md:border-[var(--border-subtle)]">
                  <dt className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)]">方法分布</dt>
                  <dd className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1.5 font-mono text-[12.5px]">
                    {methods.length > 0 ? methods.map(({ method, count }) => {
                      const m = method.toLowerCase();
                      return (
                        <span key={method} className="inline-flex items-baseline gap-1.5">
                          <span className="font-semibold tabular-nums text-[var(--text-secondary)]">{count}</span>
                          <span className="text-[11px] font-semibold tracking-wider" style={{ color: `var(--method-${m}-text, var(--text-tertiary))` }}>{method}</span>
                        </span>
                      );
                    }) : (
                      <span className="text-[var(--text-quaternary)]">无接口</span>
                    )}
                  </dd>
                </div>
              </dl>
            </div>

            {!viewingOlder && mcpStatus && (
              <div className="mt-8">
                <McpToggle
                  service={svc}
                  environment={env}
                  groupSlug={grpSlug}
                  initialEnabled={mcpStatus.enabled ?? false}
                  initialSseEndpoint={mcpStatus.sseEndpoint ?? null}
                  initialStreamableEndpoint={mcpStatus.streamableEndpoint ?? null}
                />
              </div>
            )}
          </header>
        </div>
      </div>

      {/* 接口列表 */}
      <div className="mx-auto max-w-[1100px] px-6 pb-20 pt-11 sm:px-10 sm:pt-12">
        <section>
          <div className="mb-4">
            <SectionLabel counter={data.operations.length}>接口</SectionLabel>
          </div>

          {data.operations.length === 0 ? (
            <p className="text-[14px] text-[var(--text-tertiary)]">该分组下暂无接口。</p>
          ) : (
            <ul className="docs-table-shell divide-y divide-[var(--border-subtle)] overflow-hidden rounded-xl">
              {data.operations.map((op) => (
                <li key={op.operationId}>
                  <Link
                    href={`/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}/${encodeURIComponent(op.operationId)}${querySuffix}`}
                    className="group/row flex items-start gap-4 px-4 py-3.5 transition-colors hover:bg-[var(--bg-subtle)]/55 active:translate-y-px"
                  >
                    <div className="shrink-0 pt-0.5">
                      <MethodBadge method={op.method} size="md" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <code className="block truncate font-mono text-[12.5px] text-[var(--text-tertiary)] transition-colors group-hover/row:text-[var(--text-secondary)]">
                        {op.path}
                      </code>
                      <p className="mt-0.5 text-[14.5px] font-medium text-[var(--text-primary)]">
                        {op.summary || op.operationId}
                      </p>
                      {op.description && (
                        <HtmlText
                          as="p"
                          text={op.description}
                          className="mt-1 line-clamp-1 text-[13px] leading-[1.55] text-[var(--text-secondary)]"
                        />
                      )}
                    </div>
                    <svg
                      className="mt-1.5 h-3.5 w-3.5 shrink-0 text-[var(--text-quaternary)] opacity-0 transition-opacity duration-150 group-hover/row:opacity-100"
                      viewBox="0 0 14 14"
                      fill="none"
                      aria-hidden
                    >
                      <path
                        d="M5 3l4 4-4 4"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  );
}
