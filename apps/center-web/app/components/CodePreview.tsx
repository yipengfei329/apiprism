import { codeToHtml } from "shiki";
import { CopyButton } from "./CopyButton";

/* ── 语言显示名 ───────────────────────────────────────────── */
const LANG_LABELS: Record<string, string> = {
  json: "JSON",
  java: "Java",
  javascript: "JavaScript",
  typescript: "TypeScript",
  tsx: "TSX",
  jsx: "JSX",
  python: "Python",
  go: "Go",
  rust: "Rust",
  shell: "Shell",
  bash: "Bash",
  yaml: "YAML",
  xml: "XML",
  html: "HTML",
  css: "CSS",
  sql: "SQL",
  graphql: "GraphQL",
  markdown: "Markdown",
  plaintext: "Text",
};

/* ── Props ────────────────────────────────────────────────── */
export interface CodePreviewProps {
  /** 代码字符串 */
  code: string;
  /** 语言标识，默认 json */
  language?: string;
  /** 是否显示复制按钮，默认 true */
  copyable?: boolean;
  /** 容器最大高度（px），默认 400 */
  maxHeight?: number;
  /** 额外容器 className */
  className?: string;
}

/* ── CodePreview（Async Server Component） ─────────────────── */
export async function CodePreview({
  code,
  language = "json",
  copyable = true,
  maxHeight = 400,
  className = "",
}: CodePreviewProps) {
  const html = await codeToHtml(code, {
    lang: language,
    theme: "one-dark-pro",
  });

  const langLabel = LANG_LABELS[language] ?? language.toUpperCase();

  return (
    <div
      className={`overflow-hidden rounded-xl border border-white/[0.06] bg-[#282c34] ${className}`}
    >
      {/* ── 顶栏：语言标签 + 复制按钮 ── */}
      <div className="flex items-center justify-between border-b border-white/[0.06] px-4 py-2">
        <span className="text-[11px] font-medium tracking-wide text-white/35">
          {langLabel}
        </span>
        {copyable && <CopyButton text={code} variant="dark" />}
      </div>

      {/* ── 代码区 ── */}
      <div
        className="overflow-auto text-[13px] leading-[1.75]
                   [&_pre]:!bg-transparent [&_pre]:px-4 [&_pre]:py-4
                   [&_code]:font-mono"
        style={{ maxHeight }}
        dangerouslySetInnerHTML={{ __html: html }}
      />
    </div>
  );
}
