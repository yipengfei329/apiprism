import { notFound } from "next/navigation";
import {
  getServiceSnapshot,
  getMcpServiceStatus,
  getServices,
  listRevisions,
  getRevisionSnapshot,
} from "../../lib/api";
import { Breadcrumb } from "../../components/Breadcrumb";
import { McpToggle } from "../../components/McpToggle";
import { EnvSwitcher } from "../../components/EnvSwitcher";
import { RevisionSwitcher } from "../../components/RevisionSwitcher";
import { RevisionBanner } from "../../components/RevisionBanner";
import { ServiceActions } from "./ServiceActions";
import { ServiceStats } from "../../components/ServiceStats";
import { ServiceSecuritySchemes } from "../../components/ServiceSecuritySchemes";
import { SortableGroupGrid } from "./SortableGroupGrid";
import { GroupCard } from "./GroupCard";
import { AgentDocLink } from "../../components/AgentDocLink";
import { DocsHeaderSearch } from "../../components/DocsHeaderSearch";
import { SectionLabel } from "../../components/SectionLabel";
import { CopyableMono } from "../../components/CopyableMono";


type Props = {
  params: Promise<{ service: string; environment: string }>;
  searchParams: Promise<{ revision?: string }>;
};

function fmtSync(iso: string): string {
  if (!iso) return "";
  const d = new Date(iso);
  if (isNaN(d.getTime())) return "";
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
  }
  return d.toLocaleDateString("zh-CN", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

function ServiceKicker({ service, env }: { service: string; env: string }) {
  return (
    <div className="docs-route-kicker mb-5 inline-flex max-w-full items-center gap-2.5 font-mono text-[11.5px]">
      <span className="rounded-md border px-2 py-1 font-semibold uppercase tracking-[0.12em] text-[var(--accent)]">
        service
      </span>
      <span className="truncate text-[var(--text-tertiary)]">{service}</span>
      <span className="text-[var(--text-quaternary)]">/</span>
      <EnvInlineBadge env={env} />
    </div>
  );
}

export default async function ServiceOverviewPage({ params, searchParams }: Props) {
  const { service, environment } = await params;
  const { revision: revisionParam } = await searchParams;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);

  const [snapshot, mcpStatus, allServices, revisions] = await Promise.all([
    revisionParam
      ? getRevisionSnapshot(svc, env, revisionParam)
      : getServiceSnapshot(svc, env),
    getMcpServiceStatus(svc, env),
    getServices(),
    listRevisions(svc, env),
  ]);
  if (!snapshot) notFound();

  const environments = allServices
    .filter((s) => s.name === svc)
    .map((s) => s.environment)
    .sort();

  const viewingRevision = revisionParam
    ? revisions.find((r) => r.id === revisionParam) ?? null
    : null;
  const viewingOlder = Boolean(viewingRevision && !viewingRevision.current);

  const serviceVersion = snapshot.version;
  const lastSync = snapshot.updatedAt ?? "";

  return (
    <div>
      {/* 面包屑 */}
      <div className="docs-sticky-bar sticky top-0 z-30 min-h-[var(--docs-header-height)] px-6 py-3 sm:px-10">
        <div className="mx-auto flex max-w-[1100px] items-center justify-between gap-3">
          <div className="min-w-0">
            <Breadcrumb items={[{ label: svc, icon: "service" }]} />
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <DocsHeaderSearch service={svc} environment={env} revision={revisionParam ?? null} />
            <AgentDocLink
              path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/apidocs.md`}
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
          <ServiceKicker service={svc} env={env} />

          <header>
            <div className="max-w-[820px]">
              <h1 className="text-[clamp(1.9rem,3.2vw,2.65rem)] font-semibold leading-[1.06] tracking-tight text-[var(--text-primary)]">
              {svc}
              </h1>

              <div className="mt-4 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[13px] text-[var(--text-tertiary)]">
                {serviceVersion && (
                  <span>
                    v<span className="tabular-nums">{serviceVersion}</span>
                  </span>
                )}
                {serviceVersion && lastSync && (
                  <span className="text-[var(--text-quaternary)]">·</span>
                )}
                {lastSync && <span>同步于 {fmtSync(lastSync)}</span>}
              </div>
            </div>

            <div className="mt-7">
              <ServiceStats snapshot={snapshot} />
            </div>

            {snapshot.serverUrls?.length > 0 && (
              <div className="docs-endpoint-object mt-8 overflow-hidden rounded-xl">
                <div className="border-b border-[var(--border-subtle)] px-5 py-3 text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)] sm:px-6">
                  Base URLs
                </div>
                <div className="flex flex-wrap gap-2 px-5 py-4 sm:px-6">
                  {snapshot.serverUrls.map((url) => (
                    <CopyableMono key={url} value={url} size="lg" ariaLabel={`复制服务地址 ${url}`} />
                  ))}
                </div>
              </div>
            )}

            <div className="mt-7 border-t border-[var(--border-subtle)] pt-5">
              <div className="flex flex-wrap items-center gap-2">
                <EnvSwitcher service={svc} currentEnv={env} environments={environments} />
                <RevisionSwitcher
                  service={svc}
                  environment={env}
                  revisions={revisions}
                  viewingRevisionId={viewingOlder ? revisionParam ?? null : null}
                />
              </div>
            </div>
          </header>
        </div>
      </div>

      <div className="mx-auto max-w-[1100px] px-6 pb-20 pt-11 sm:px-10 sm:pt-12">
      {/* ─── 接口分组 ─── */}
      <section className="mb-14">
        <div className="mb-5">
          <SectionLabel counter={snapshot.groups.length}>分组</SectionLabel>
        </div>

        {snapshot.groups.length === 0 ? (
          <p className="text-[14px] text-[var(--text-tertiary)]">该服务下暂无接口分组。</p>
        ) : viewingOlder ? (
          <div className="grid auto-rows-fr gap-3 md:grid-cols-2">
            {snapshot.groups.map((group) => {
              const querySuffix = revisionParam
                ? `?revision=${encodeURIComponent(revisionParam)}`
                : "";
              const href = `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(group.slug)}${querySuffix}`;
              return (
                <GroupCard
                  key={group.slug}
                  group={group}
                  href={href}
                  agentDocPath={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(group.slug)}/apidocs.md`}
                />
              );
            })}
          </div>
        ) : (
          <SortableGroupGrid service={svc} environment={env} groups={snapshot.groups} />
        )}
      </section>

      {/* ─── MCP 网关 ─── */}
      {!viewingOlder && (
        <section className="mb-14">
          <div className="mb-4">
            <SectionLabel>MCP 网关</SectionLabel>
          </div>
          <McpToggle
            service={svc}
            environment={env}
            initialEnabled={mcpStatus?.serviceEnabled ?? false}
            initialSseEndpoint={mcpStatus?.sseEndpoint ?? null}
            initialStreamableEndpoint={mcpStatus?.streamableEndpoint ?? null}
          />
        </section>
      )}

      {/* ─── 认证方式 ─── */}
      <ServiceSecuritySchemes securitySchemes={snapshot.securitySchemes ?? {}} />

      {/* ─── 危险操作 ─── */}
        {!viewingOlder && (
          <section className="mt-14 border-t border-[var(--border-subtle)] pt-10">
            <div className="mb-4">
              <SectionLabel>危险操作</SectionLabel>
            </div>
            <ServiceActions service={svc} environment={env} />
          </section>
        )}
      </div>
    </div>
  );
}

/**
 * 副标题里使用的紧凑 env 标记：状态点（带环）+ 文字
 * 比 EnvBadge 整体的 chip 更轻，只在已经有上下文时使用
 */
function EnvInlineBadge({ env }: { env: string }) {
  const lower = env.toLowerCase();
  let color = "var(--env-default-dot)";
  let textColor = "var(--text-secondary)";
  if (lower === "production" || lower === "prod") {
    color = "var(--env-prod-dot)";
    textColor = "var(--env-prod-text)";
  } else if (lower === "staging" || lower === "preview" || lower === "pre") {
    color = "var(--env-staging-dot)";
    textColor = "var(--env-staging-text)";
  } else if (lower === "test" || lower === "testing") {
    color = "var(--env-test-dot)";
    textColor = "var(--env-test-text)";
  }
  return (
    <span className="inline-flex items-center gap-1.5">
      <span className="docs-status-dot" style={{ color }} aria-hidden />
      <span className="font-mono text-[11.5px] font-medium uppercase tracking-wider" style={{ color: textColor }}>
        {env}
      </span>
    </span>
  );
}
