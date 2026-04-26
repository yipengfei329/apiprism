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
import { McpToggle } from "../../../components/McpToggle";
import { RevisionBanner } from "../../../components/RevisionBanner";
import { SectionLabel } from "../../../components/SectionLabel";


type Props = {
  params: Promise<{ service: string; environment: string; group: string }>;
  searchParams: Promise<{ revision?: string }>;
};

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

  return (
    <div>
      {/* 面包屑 sticky */}
      <div className="sticky top-0 z-10 border-b border-[var(--border-default)] bg-[var(--bg-canvas)]/85 px-6 py-3 backdrop-blur-xl sm:px-10">
        <div className="mx-auto max-w-[1100px]">
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

      {/* 分组头部 */}
      <div className="mx-auto max-w-[1100px] px-6 pt-12 sm:px-10 sm:pt-16">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <h1
              className="text-[clamp(1.7rem,2.6vw,2.4rem)] font-semibold leading-[1.1] text-[var(--text-primary)]"
              style={{ letterSpacing: "-0.03em" }}
            >
              {data.name}
            </h1>
            <p className="mt-3 text-[13px] text-[var(--text-tertiary)]">
              <span className="font-semibold tabular-nums text-[var(--text-secondary)]">
                {data.operations.length}
              </span>{" "}
              个接口
            </p>
          </div>
          <AgentDocLink
            path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}/apidocs.md`}
          />
        </div>
        {data.description && (
          <HtmlText
            as="div"
            text={data.description}
            className="mt-4 max-w-[68ch] text-[15px] leading-[1.7] text-[var(--text-secondary)] [&>p]:mt-2.5 [&>p:first-child]:mt-0 [&_ul]:mt-1.5 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mt-1.5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mt-0.5 [&_code]:rounded [&_code]:bg-[var(--bg-subtle)] [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[13px]"
          />
        )}
      </div>

      {/* MCP 分组开关 */}
      {!viewingOlder && mcpStatus && (
        <div className="mx-auto max-w-[1100px] px-6 pt-10 sm:px-10">
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

      {/* 接口列表 */}
      <div className="mx-auto max-w-[1100px] px-6 pb-20 pt-12 sm:px-10">
        <section>
          <div className="mb-4">
            <SectionLabel counter={data.operations.length}>接口</SectionLabel>
          </div>

          {data.operations.length === 0 ? (
            <p className="text-[14px] text-[var(--text-tertiary)]">该分组下暂无接口。</p>
          ) : (
            <ul className="divide-y divide-[var(--border-subtle)] border-y border-[var(--border-subtle)]">
              {data.operations.map((op) => (
                <li key={op.operationId}>
                  <Link
                    href={`/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}/${encodeURIComponent(op.operationId)}${querySuffix}`}
                    className="group/row flex items-start gap-4 px-4 py-3.5 transition-colors hover:bg-[var(--bg-subtle)]"
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
