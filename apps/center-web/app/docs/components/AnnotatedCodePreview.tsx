import { codeToHtml } from "shiki";
import { CopyButton } from "@/app/components/CopyButton";
import type { JsonSchema } from "../lib/api";
import { buildLineAnnotations } from "./buildLineAnnotations";
import { CodeAnnotationPanel } from "./CodeAnnotationPanel";

const LANG_LABELS: Record<string, string> = {
  json: "JSON",
  java: "Java",
  javascript: "JavaScript",
  typescript: "TypeScript",
  yaml: "YAML",
  xml: "XML",
};

export interface AnnotatedCodePreviewProps {
  /** 代码字符串 */
  code: string;
  /** 语言标识 */
  language?: string;
  /** JsonSchema，用于自动生成行级注释 */
  schema?: JsonSchema | null;
  /** 容器最大高度 */
  maxHeight?: number;
  /** 额外容器 className */
  className?: string;
}

/**
 * 带注释面板的代码预览（Async Server Component）。
 * 当 schema 包含字段 description 时，右侧自动展示逐行注释；
 * 无注释时退化为普通代码预览。
 */
export async function AnnotatedCodePreview({
  code,
  language = "json",
  schema,
  maxHeight = 400,
  className = "",
}: AnnotatedCodePreviewProps) {
  const [htmlLight, htmlDark] = await Promise.all([
    codeToHtml(code, { lang: language, theme: "github-light" }),
    codeToHtml(code, { lang: language, theme: "one-dark-pro" }),
  ]);

  const totalLines = code.split("\n").length;
  const annotationMap = schema
    ? buildLineAnnotations(code, schema)
    : new Map<number, string>();
  const hasAnnotations = annotationMap.size > 0;
  // Map → 可序列化数组，传递给客户端组件
  const annotations: [number, string][] = Array.from(annotationMap.entries());

  const langLabel = LANG_LABELS[language] ?? language.toUpperCase();

  return (
    <div
      className={`overflow-hidden rounded-xl border border-[var(--border-default)] bg-[var(--code-bg)] ${className}`}
    >
      {/* 顶栏 */}
      <div className="flex items-center justify-between border-b border-[var(--code-border)] px-4 py-2">
        <div className="flex items-center gap-3">
          <span className="text-[11px] font-medium text-[var(--code-label)]">
            示例预览 · 自动生成
          </span>
          <span className="text-[11px] font-medium tracking-wide text-[var(--code-label)]">
            {langLabel}
          </span>
        </div>
        <CopyButton text={code} variant="dark" />
      </div>

      {/* 内容区 */}
      {hasAnnotations ? (
        <CodeAnnotationPanel
          codeHtmlLight={htmlLight}
          codeHtmlDark={htmlDark}
          annotations={annotations}
          totalLines={totalLines}
          maxHeight={maxHeight}
        />
      ) : (
        <div
          className="overflow-auto text-[13px] leading-[1.75]
                     [&_pre]:!bg-transparent [&_pre]:px-4 [&_pre]:py-4
                     [&_code]:font-mono"
          style={{ maxHeight }}
        >
          <div className="dark:hidden" dangerouslySetInnerHTML={{ __html: htmlLight }} />
          <div className="hidden dark:block" dangerouslySetInnerHTML={{ __html: htmlDark }} />
        </div>
      )}
    </div>
  );
}
