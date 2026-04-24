import { notFound } from "next/navigation";
import { getOperation, getRevisionOperation, getServiceSnapshot, getRevisionSnapshot, listRevisions } from "../../../../lib/api";
import { OperationDetail } from "../../../../components/OperationDetail";
import { OperationWiki } from "../../../../components/OperationWiki";
import { Breadcrumb } from "../../../../components/Breadcrumb";
import { RevisionBanner } from "../../../../components/RevisionBanner";


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
      <div className="sticky top-0 z-10 border-b border-v-gray-100 bg-[var(--bg-surface)] px-4 py-3 sm:px-8">
        <div className="mx-auto max-w-[1100px]">
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
      <OperationDetail op={op} service={svc} environment={env} group={grpSlug}>
        <OperationWiki op={op} securitySchemes={snapshot?.securitySchemes} />
      </OperationDetail>
    </div>
  );
}
