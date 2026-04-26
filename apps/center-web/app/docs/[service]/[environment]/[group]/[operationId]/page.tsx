import { notFound } from "next/navigation";
import { getOperation, getRevisionOperation, getServiceSnapshot, getRevisionSnapshot, listRevisions } from "../../../../lib/api";
import { OperationDetail } from "../../../../components/OperationDetail";
import { OperationWiki } from "../../../../components/OperationWiki";
import { CurlPanel } from "../../../../components/CurlPanel";
import { Breadcrumb } from "../../../../components/Breadcrumb";
import { RevisionBanner } from "../../../../components/RevisionBanner";
import { AgentDocLink } from "../../../../components/AgentDocLink";
import { DocsHeaderSearch } from "../../../../components/DocsHeaderSearch";


type Props = {
  params: Promise<{ service: string; environment: string; group: string; operationId: string }>;
  searchParams: Promise<{ revision?: string }>;
};

export default async function OperationPage({ params, searchParams }: Props) {
  const { service, environment, group, operationId } = await params;
  const { revision: revisionParam } = await searchParams;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);
  const grpSlug = decodeURIComponent(group);
  const opId = decodeURIComponent(operationId);

  const [op, snapshot, revisions] = await Promise.all([
    revisionParam
      ? getRevisionOperation(svc, env, revisionParam, opId)
      : getOperation(svc, env, opId),
    revisionParam
      ? getRevisionSnapshot(svc, env, revisionParam)
      : getServiceSnapshot(svc, env),
    revisionParam ? listRevisions(svc, env) : Promise.resolve([]),
  ]);
  if (!op) notFound();

  const viewingRevision = revisionParam
    ? revisions.find((r) => r.id === revisionParam) ?? null
    : null;
  const viewingOlder = Boolean(viewingRevision && !viewingRevision.current);
  const querySuffix = revisionParam
    ? `?revision=${encodeURIComponent(revisionParam)}`
    : "";

  return (
    <div>
      {/* 面包屑导航：sticky，毛玻璃 */}
      <div className="docs-sticky-bar sticky top-0 z-30 min-h-[var(--docs-header-height)] px-4 py-3 sm:px-8">
        <div className="mx-auto flex max-w-[1100px] items-center justify-between gap-3">
          <div className="min-w-0">
            <Breadcrumb
              items={[
                {
                  label: svc,
                  href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}${querySuffix}`,
                  icon: "service",
                },
                {
                  label: grpSlug,
                  href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}${querySuffix}`,
                  icon: "group",
                },
                {
                  label: op.summary,
                  icon: "operation",
                },
              ]}
            />
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <DocsHeaderSearch service={svc} environment={env} revision={revisionParam ?? null} />
            <AgentDocLink path={`/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}/${encodeURIComponent(op.operationId)}/apidocs.md`} />
          </div>
        </div>
      </div>

      {viewingOlder && viewingRevision && (
        <div className="mx-auto max-w-[1100px] px-4 pt-4 sm:px-8">
          <RevisionBanner
            service={svc}
            environment={env}
            revisionId={viewingRevision.id}
            seq={viewingRevision.seq}
            registeredAt={viewingRevision.registeredAt}
          />
        </div>
      )}

      {/* 接口详情：头部 + 选项卡 + 内容 */}
      <OperationDetail
        op={op}
        debugPanel={
          <CurlPanel
            op={op}
            serverUrls={snapshot?.serverUrls ?? []}
            securitySchemes={snapshot?.securitySchemes ?? {}}
          />
        }
      >
        <OperationWiki op={op} securitySchemes={snapshot?.securitySchemes} />
      </OperationDetail>
    </div>
  );
}
