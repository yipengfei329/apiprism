import { notFound } from "next/navigation";
import { getOperation } from "../../../../lib/api";
import { OperationDetail } from "../../../../components/OperationDetail";
import { OperationWiki } from "../../../../components/OperationWiki";
import { Breadcrumb, type BreadcrumbItem } from "../../../../components/Breadcrumb";


type Props = {
  params: Promise<{ service: string; environment: string; operationId: string }>;
};

export default async function OperationPage({ params }: Props) {
  const { service, environment, operationId } = await params;
  const svc = decodeURIComponent(service);
  const env = decodeURIComponent(environment);
  const opId = decodeURIComponent(operationId);

  const op = await getOperation(svc, env, opId);
  if (!op) notFound();

  // 从第一个 tag 推断所属分组（用于面包屑）
  const group = op.tags?.[0];

  return (
    <div>
      {/* 面包屑导航：sticky，毛玻璃 */}
      <div className="sticky top-0 z-10 px-8 py-3 v-glass" style={{ borderRadius: 0, borderLeft: "none", borderRight: "none", borderTop: "none" }}>
        <div className="mx-auto max-w-[1100px]">
          <Breadcrumb
            items={[
              {
                label: svc,
                href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}`,
                icon: "service",
              },
              ...(group
                ? [
                    {
                      label: group,
                      href: `/docs/${encodeURIComponent(svc)}/${encodeURIComponent(env)}/groups/${encodeURIComponent(group)}`,
                      icon: "group" as const,
                    },
                  ]
                : []),
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
