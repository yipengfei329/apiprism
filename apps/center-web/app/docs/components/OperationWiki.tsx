import { CanonicalOperation, CanonicalSecurityScheme } from "../lib/api";
import { SchemaTable } from "./SchemaTable";
import { schemaTypeLabel } from "./schemaUtils";
import { generateExample } from "./generateExample";
import { AnnotatedCodePreview } from "./AnnotatedCodePreview";
import { ResponseTabs } from "./ResponseTabs";
import { OnThisPage, type SectionItem } from "./OnThisPage";
import { ParameterTable } from "./ParameterTable";
import { RequestBodyTabs } from "./RequestBodyTabs";
import { SecuritySchemeBadge } from "./SecuritySchemeBadge";

// ── 必填 / 选填标记 ──

function RequiredTag({ required }: { required: boolean }) {
  return required ? (
    <span className="font-mono text-[10px] font-semibold text-[var(--danger)]">必填</span>
  ) : (
    <span className="font-mono text-[10px] font-medium text-v-gray-400">选填</span>
  );
}

// ── 分区标题（极简：单行 H2，靠字重和留白构建层级） ──

function SectionTitle({ children }: { children: React.ReactNode }) {
  return (
    <h2
      className="mb-5 text-[15px] font-semibold text-[var(--text-primary)]"
      style={{ letterSpacing: "-0.01em" }}
    >
      {children}
    </h2>
  );
}

// ── 计算页面有哪些可跳转分区 ──

function hasRequestBodyContent(op: CanonicalOperation): boolean {
  // 仅当存在结构定义时才算有内容；空 body 的 contentType 属于噪音，不展示
  return Boolean(op.requestBody?.schema);
}

function buildSectionList(op: CanonicalOperation): SectionItem[] {
  const sections: SectionItem[] = [];
  if (op.parameters && op.parameters.length > 0) {
    sections.push({ id: "parameters", label: "请求参数" });
  }
  if (hasRequestBodyContent(op)) {
    sections.push({ id: "request-body", label: "请求体" });
  }
  if (op.responses && op.responses.length > 0) {
    sections.push({ id: "responses", label: "响应" });
  }
  if (op.securityRequirements && op.securityRequirements.length > 0) {
    sections.push({ id: "security", label: "安全认证" });
  }
  if (op.tags && op.tags.length > 0) {
    sections.push({ id: "tags", label: "标签" });
  }
  return sections;
}

// ── 文档 Tab 内容 ──

export async function OperationWiki({
  op,
  securitySchemes,
}: {
  op: CanonicalOperation;
  securitySchemes?: Record<string, CanonicalSecurityScheme>;
}) {
  const sections = buildSectionList(op);

  // 服务端预生成请求体示例
  const requestBodyExample =
    op.requestBody?.schema && op.requestBody.contentType?.includes("json")
      ? JSON.stringify(generateExample(op.requestBody.schema), null, 2)
      : null;

  // 服务端预生成所有响应示例（有 schema 即生成，不限 contentType）
  const responseExamples = op.responses?.map((r) => {
    if (!r.schema) return null;
    return JSON.stringify(generateExample(r.schema), null, 2);
  });

  return (
    <div className="flex gap-12">
      {/* ── 左侧：主内容 ── */}
      <article className="min-w-0 flex-1 [&>section+section]:border-t [&>section+section]:border-[var(--border-subtle)] [&>section]:pb-8 [&>section+section]:pt-8 [&>section:last-of-type]:pb-0">
        {/* ── 参数 ── */}
        {op.parameters && op.parameters.length > 0 && (
          <section id="parameters" className="scroll-mt-16">
            <SectionTitle>请求参数</SectionTitle>
            <ParameterTable parameters={op.parameters} />
          </section>
        )}

        {/* ── 请求体 ── */}
        {op.requestBody?.schema && (
          <section id="request-body" className="scroll-mt-16">
            <SectionTitle>请求体</SectionTitle>
            <div className="mb-4 flex flex-wrap items-center gap-2.5">
              <RequiredTag required={op.requestBody.required} />
              {op.requestBody.contentType && (
                <code className="rounded-full bg-[var(--bg-subtle)] px-2.5 py-1 font-mono text-[11px] text-v-gray-500">
                  {op.requestBody.contentType}
                </code>
              )}
              <span className="rounded-full bg-[var(--bg-subtle)] px-2.5 py-1 font-mono text-[11px] text-v-link">
                {schemaTypeLabel(op.requestBody.schema)}
              </span>
            </div>
            <RequestBodyTabs
              schemaPanel={
                <div className="px-5 py-1">
                  <SchemaTable schema={op.requestBody.schema} />
                </div>
              }
              examplePanel={
                requestBodyExample ? (
                  <AnnotatedCodePreview
                    code={requestBodyExample}
                    language="json"
                    schema={op.requestBody.schema}
                    className="!rounded-none !border-0"
                  />
                ) : undefined
              }
            />
          </section>
        )}

        {/* ── 响应 ── */}
        {op.responses && op.responses.length > 0 && (
          <section id="responses" className="scroll-mt-16">
            <SectionTitle>响应</SectionTitle>
            <ResponseTabs
              responses={op.responses}
              examples={responseExamples?.map((code, i) =>
                code ? (
                  <AnnotatedCodePreview key={i} code={code} language="json" schema={op.responses?.[i]?.schema} className="!rounded-none !border-0" />
                ) : null,
              )}
            />
          </section>
        )}

        {/* ── 安全要求 ── */}
        {op.securityRequirements && op.securityRequirements.length > 0 && (
          <section id="security" className="scroll-mt-16">
            <SectionTitle>安全认证</SectionTitle>
            <div className="flex flex-wrap gap-2.5">
              {op.securityRequirements.map((s) => (
                <SecuritySchemeBadge
                  key={s}
                  name={s}
                  scheme={securitySchemes?.[s]}
                />
              ))}
            </div>
          </section>
        )}

        {/* ── Tags ── */}
        {op.tags && op.tags.length > 0 && (
          <section id="tags" className="scroll-mt-16">
            <SectionTitle>标签</SectionTitle>
            <div className="flex flex-wrap gap-2.5">
              {op.tags.map((t) => (
                <span
                  key={t}
                  className="rounded-full bg-v-gray-50 px-3.5 py-1.5 text-[13px] font-medium text-v-link"
                >
                  {t}
                </span>
              ))}
            </div>
          </section>
        )}
      </article>

      {/* ── 右侧：页内导航 ── */}
      {sections.length > 0 && (
        <aside className="hidden w-[200px] shrink-0 xl:block">
          <OnThisPage sections={sections} />
        </aside>
      )}
    </div>
  );
}
