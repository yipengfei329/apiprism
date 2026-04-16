import { notFound } from "next/navigation";
import { getOperation } from "../../../../lib/api";
import { OperationDetail } from "../../../../components/OperationDetail";
import { OperationWiki } from "../../../../components/OperationWiki";
import { Breadcrumb } from "../../../../components/Breadcrumb";


type Props = {
  params: Promise<{ service: string; environment: string; group: string; operationId: string }>;
};

export default async function OperationPage({ params }: Props) {
  const { service, environment, group, operationId } = await params;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);
  const grpSlug = decodeURIComponent(group);
  const opId = decodeURIComponent(operationId);

  const op = await getOperation(svc, env, opId);
  if (!op) notFound();

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
                label: grpSlug,
                href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/${encodeURIComponent(grpSlug)}`,
                icon: "group",
              },
              {
                label: op.summary || opId,
                icon: "operation",
              },
            ]}
          />
        </div>
      </div>

      {/* 接口详情：头部 + 选项卡 + 内容 */}
      <OperationDetail op={op} service={svc} environment={env}>
        <OperationWiki op={op} />
      </OperationDetail>
    </div>
  );
}
