import Link from "next/link";
import { notFound } from "next/navigation";
import {
  getServiceSnapshot,
  getMcpServiceStatus,
  getServices,
  listRevisions,
  getRevisionSnapshot,
} from "../../lib/api";
import { Breadcrumb } from "../../components/Breadcrumb";
import { HtmlText } from "../../components/HtmlText";
import { McpToggle } from "../../components/McpToggle";
import { EnvSwitcher } from "../../components/EnvSwitcher";
import { RevisionSwitcher } from "../../components/RevisionSwitcher";
import { RevisionBanner } from "../../components/RevisionBanner";
import { ServiceActions } from "./ServiceActions";
import { ServiceStats } from "../../components/ServiceStats";
import { ServiceSecuritySchemes } from "../../components/ServiceSecuritySchemes";
import { SortableGroupGrid } from "./SortableGroupGrid";
import { AgentDocLink } from "../../components/AgentDocLink";


type Props = {
  params: Promise<{ service: string; environment: string }>;
  searchParams: Promise<{ revision?: string }>;
};

export default async function ServiceOverviewPage({ params, searchParams }: Props) {
  const { service, environment } = await params;
  const { revision: revisionParam } = await searchParams;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);

  // 并行拉取：当前/历史 snapshot、MCP 状态、服务列表（环境切换用）、版本历史列表
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

      {viewingOlder && viewingRevision && (
        <RevisionBanner
          service={svc}
          environment={env}
          revisionId={viewingRevision.id}
          seq={viewingRevision.seq}
          registeredAt={viewingRevision.registeredAt}
        />
      )}

      {/* 服务头部 */}
      <header className="mb-14">
        <div className="flex items-start justify-between gap-4">
          <h1
            className="text-[clamp(1.8rem,3vw,2.6rem)] font-semibold leading-tight text-[var(--text-primary)]"
            style={{ letterSpacing: "-0.035em" }}
          >
            {svc}
          </h1>
          <div className="mt-1 shrink-0">
            <AgentDocLink
              path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/apidocs.md`}
            />
          </div>
        </div>
        <div className="mt-4 flex flex-wrap items-center gap-2">
          <EnvSwitcher service={svc} currentEnv={env} environments={environments} />
          {environments.length <= 1 && (
            <span className="rounded-md bg-v-gray-50 px-2.5 py-0.5 font-mono text-[12px] font-medium text-v-gray-500">
              {env}
            </span>
          )}
          <RevisionSwitcher
            service={svc}
            environment={env}
            revisions={revisions}
            viewingRevisionId={viewingOlder ? revisionParam ?? null : null}
          />
        </div>

        {snapshot.serverUrls?.length > 0 && (
          <div className="mt-8">
            <p className="mb-3 text-[12px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
              服务地址
            </p>
            <div className="flex flex-wrap gap-2">
              {snapshot.serverUrls.map((url) => (
                <code
                  key={url}
                  className="rounded-lg bg-v-gray-50 px-3 py-1.5 font-mono text-[13px] text-[var(--text-primary)]"
                >
                  {url}
                </code>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* 统计面板 */}
      <ServiceStats snapshot={snapshot} />

      {/* 认证方式 */}
      <ServiceSecuritySchemes securitySchemes={snapshot.securitySchemes ?? {}} />

      {/* MCP 服务开关：查看旧版本时隐藏，避免基于过期快照操作 */}
      {!viewingOlder && (
        <section className="mb-14">
          <p className="mb-4 text-[12px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
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
      )}

      {/* 接口分组 */}
      <section>
        <p className="mb-6 text-[12px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
          接口分组 — {snapshot.groups.length}
        </p>

        {snapshot.groups.length === 0 ? (
          <p className="text-[var(--text-tertiary)]">该服务下暂无接口分组。</p>
        ) : viewingOlder ? (
          // 历史版本：只读，分组卡片保留 revision 查询参数，不展示拖拽手柄
          <div className="grid gap-3 sm:grid-cols-2">
            {snapshot.groups.map((group) => {
              const querySuffix = revisionParam
                ? `?revision=${encodeURIComponent(revisionParam)}`
                : "";
              const href = `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(group.slug)}${querySuffix}`;
              return (
                <Link
                  key={group.name}
                  href={href}
                  className="group rounded-xl bg-[var(--bg-surface)] p-5 transition-all v-card-full v-card-full-hover"
                >
                  <h3
                    className="font-semibold text-[var(--text-primary)]"
                    style={{ letterSpacing: "-0.015em" }}
                  >
                    {group.name}
                  </h3>
                  {group.description && (
                    <HtmlText
                      as="p"
                      text={group.description}
                      className="mt-1.5 line-clamp-2 text-[13px] leading-[1.65] text-[var(--text-secondary)]"
                    />
                  )}
                  <div className="mt-4 flex items-center justify-between">
                    <p className="font-mono text-[12px] text-[var(--text-tertiary)]">
                      {group.operations.length} 个接口
                    </p>
                    <svg
                      className="h-3.5 w-3.5 text-[var(--text-tertiary)] opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:opacity-100"
                      viewBox="0 0 14 14"
                      fill="none"
                    >
                      <path d="M3 7h8M7.5 3.5L11 7l-3.5 3.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </div>
                </Link>
              );
            })}
          </div>
        ) : (
          // 当前版本：支持拖拽排序
          <SortableGroupGrid
            service={svc}
            environment={env}
            groups={snapshot.groups}
          />
        )}
      </section>

      {/* 危险区域：删除环境 */}
      {!viewingOlder && (
        <section className="mt-14 border-t border-[var(--border-subtle)] pt-14">
          <p className="mb-4 text-[12px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
            危险操作
          </p>
          <ServiceActions service={svc} environment={env} />
        </section>
      )}
    </div>
  );
}
