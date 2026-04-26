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
    <div className="mx-auto max-w-[860px] px-6 pb-20 pt-12 sm:px-10 sm:pt-16">
      {/* 面包屑 */}
      <div className="mb-8">
        <Breadcrumb items={[{ label: svc, icon: "service" }]} />
      </div>

      {viewingOlder && viewingRevision && (
        <RevisionBanner
          service={svc}
          environment={env}
          revisionId={viewingRevision.id}
          seq={viewingRevision.seq}
          registeredAt={viewingRevision.registeredAt}
        />
      )}

      {/* ─── 服务头部 ─── */}
      <header className="mb-12">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <h1
              className="text-[clamp(1.9rem,3.2vw,2.8rem)] font-semibold leading-[1.05] text-[var(--text-primary)]"
              style={{ letterSpacing: "-0.035em" }}
            >
              {svc}
            </h1>

            {/* 副标题：env · v1.0 · 同步于 14:32 ——单行小灰，但 env 是真组件 */}
            <div className="mt-3.5 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[13px] text-[var(--text-tertiary)]">
              {/* 在副标题里嵌一个真 env badge，比纯文字立体 */}
              <EnvInlineBadge env={env} />
              {serviceVersion && (
                <>
                  <span className="text-[var(--text-quaternary)]">·</span>
                  <span>
                    v<span className="tabular-nums">{serviceVersion}</span>
                  </span>
                </>
              )}
              {lastSync && (
                <>
                  <span className="text-[var(--text-quaternary)]">·</span>
                  <span>同步于 {fmtSync(lastSync)}</span>
                </>
              )}
            </div>
          </div>
          <div className="mt-1 shrink-0">
            <AgentDocLink
              path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/apidocs.md`}
            />
          </div>
        </div>

        {/* 服务地址：直接展示在头部，hover 出 copy ——程序员第一眼想抓的信息 */}
        {snapshot.serverUrls?.length > 0 && (
          <div className="mt-5 flex flex-wrap gap-2">
            {snapshot.serverUrls.map((url) => (
              <CopyableMono key={url} value={url} size="lg" ariaLabel={`复制服务地址 ${url}`} />
            ))}
          </div>
        )}

        {/* 切换器单独一行 */}
        <div className="mt-6 flex flex-wrap items-center gap-2">
          <EnvSwitcher service={svc} currentEnv={env} environments={environments} />
          <RevisionSwitcher
            service={svc}
            environment={env}
            revisions={revisions}
            viewingRevisionId={viewingOlder ? revisionParam ?? null : null}
          />
        </div>
      </header>

      {/* ─── 统计概览 ─── */}
      <ServiceStats snapshot={snapshot} />

      {/* ─── 接口分组 ─── */}
      <section className="mb-14">
        <div className="mb-5">
          <SectionLabel counter={snapshot.groups.length}>分组</SectionLabel>
        </div>

        {snapshot.groups.length === 0 ? (
          <p className="text-[14px] text-[var(--text-tertiary)]">该服务下暂无接口分组。</p>
        ) : viewingOlder ? (
          <div className="grid gap-3 sm:grid-cols-2">
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
