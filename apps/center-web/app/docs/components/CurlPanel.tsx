"use client";

import { useState, useEffect, useRef } from "react";
import { ArrowCounterClockwise, Terminal } from "@phosphor-icons/react";
import { getSingletonHighlighter, type Highlighter } from "shiki";
import { CopyButton } from "@/app/components/CopyButton";
import { type CanonicalOperation, type CanonicalSecurityScheme } from "../lib/api";
import { generateExample } from "./generateExample";

// ── Shiki 单例：只加载 shell 语法 + 双主题，避免重复初始化 ────────────────────
let highlighterPromise: Promise<Highlighter> | null = null;
function loadHighlighter(): Promise<Highlighter> {
  if (!highlighterPromise) {
    highlighterPromise = getSingletonHighlighter({
      themes: ["github-light", "one-dark-pro"],
      langs: ["shellscript"],
    });
  }
  return highlighterPromise;
}

// ── curl 命令生成 ────────────────────────────────────────────────────────────

function buildCurlCommand(
  op: CanonicalOperation,
  serverUrl: string,
  securitySchemes: Record<string, CanonicalSecurityScheme>
): string {
  const method = op.method.toUpperCase();

  // 路径参数替换
  let urlPath = op.path;
  for (const param of op.parameters.filter((p) => p.location === "path")) {
    const val = generateExample(param.schema) ?? `{${param.name}}`;
    urlPath = urlPath.replace(`{${param.name}}`, String(val));
  }

  // Query 参数
  const queryParts = op.parameters
    .filter((p) => p.location === "query")
    .map((p) => {
      const val = generateExample(p.schema) ?? `<${p.name}>`;
      return `${encodeURIComponent(p.name)}=${encodeURIComponent(String(val))}`;
    });

  const fullUrl =
    serverUrl + urlPath + (queryParts.length ? `?${queryParts.join("&")}` : "");

  // 请求头
  const headers: string[] = [];

  // 鉴权头 —— 取第一个有效的 security requirement
  for (const key of op.securityRequirements) {
    const scheme = securitySchemes[key];
    if (!scheme) continue;
    if (scheme.type === "http") {
      if (scheme.scheme?.toLowerCase() === "bearer") {
        headers.push("Authorization: Bearer <your-token>");
      } else if (scheme.scheme?.toLowerCase() === "basic") {
        headers.push("Authorization: Basic <base64-credentials>");
      }
    } else if (scheme.type === "apiKey" && scheme.in === "header" && scheme.paramName) {
      headers.push(`${scheme.paramName}: <your-api-key>`);
    } else if (scheme.type === "oauth2" || scheme.type === "openIdConnect") {
      headers.push("Authorization: Bearer <your-token>");
    }
    break;
  }

  // header 位置的参数
  for (const param of op.parameters.filter((p) => p.location === "header")) {
    const val = generateExample(param.schema) ?? `<${param.name}>`;
    headers.push(`${param.name}: ${String(val)}`);
  }

  // Content-Type
  if (op.requestBody) {
    headers.push(`Content-Type: ${op.requestBody.contentType || "application/json"}`);
  }

  // 请求体
  let body: string | null = null;
  if (op.requestBody?.schema) {
    body = JSON.stringify(generateExample(op.requestBody.schema), null, 2);
  } else if (op.requestBody) {
    body = "{}";
  }

  // 拼装 curl 各段，用 " \\\n" 连接，最后一段不带 backslash
  const parts: string[] = [`curl -X ${method}`, `  '${fullUrl}'`];
  for (const h of headers) {
    parts.push(`  -H '${h}'`);
  }
  if (body !== null) {
    parts.push(`  -d '${body}'`);
  }

  return parts.join(" \\\n");
}

// ── 组件 ─────────────────────────────────────────────────────────────────────

interface CurlPanelProps {
  op: CanonicalOperation;
  serverUrls: string[];
  securitySchemes: Record<string, CanonicalSecurityScheme>;
}

export function CurlPanel({ op, serverUrls, securitySchemes }: CurlPanelProps) {
  const serverUrl = serverUrls[0] ?? "{BASE_URL}";

  // 初始命令只计算一次
  const [initialCommand] = useState(() =>
    buildCurlCommand(op, serverUrl, securitySchemes)
  );
  const [value, setValue] = useState(initialCommand);
  const [highlighted, setHighlighted] = useState<{ light: string; dark: string } | null>(
    null
  );
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Shiki 高亮，跟随 value 变化重新生成 HTML
  useEffect(() => {
    let cancelled = false;
    loadHighlighter()
      .then((hl) => {
        if (cancelled) return;
        const opts = { lang: "shellscript", theme: "" };
        const light = hl.codeToHtml(value, { ...opts, theme: "github-light" });
        const dark = hl.codeToHtml(value, { ...opts, theme: "one-dark-pro" });
        setHighlighted({ light, dark });
      })
      .catch(() => {
        // Shiki 加载失败时静默退化为纯文本，textarea 仍可正常使用
      });
    return () => {
      cancelled = true;
    };
  }, [value]);

  // textarea 自动伸展
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = el.scrollHeight + "px";
  }, [value]);

  const isDirty = value !== initialCommand;
  const hasHighlight = highlighted !== null;

  return (
    <div className="space-y-3">
      {/* 区块标题 */}
      <div className="flex items-center gap-2">
        <Terminal size={14} className="text-v-gray-400" />
        <span className="text-[13px] font-medium text-v-gray-500">cURL 示范</span>
        <span className="text-[11px] text-v-gray-300">· 填入真实参数后复制执行</span>
      </div>

      {/* 代码区块 */}
      <div className="overflow-hidden rounded-xl border border-[var(--code-border)] bg-[var(--code-bg)]">
        {/* 顶栏 */}
        <div className="flex items-center justify-between border-b border-[var(--code-border)] px-4 py-2">
          <span className="text-[11px] font-medium tracking-wide text-[var(--code-label)]">
            Shell
          </span>
          <div className="flex items-center gap-1">
            {isDirty && (
              <button
                type="button"
                onClick={() => setValue(initialCommand)}
                className="flex cursor-pointer select-none items-center gap-1 rounded-md px-2 py-1
                           text-[11px] font-medium text-[var(--code-label)] transition-colors duration-150
                           hover:bg-[var(--bg-hover)] hover:text-[var(--text-secondary)]"
                title="重置为生成示例"
              >
                <ArrowCounterClockwise size={12} />
                <span>重置</span>
              </button>
            )}
            <CopyButton text={value} variant="light" />
          </div>
        </div>

        {/* 高亮 + 编辑双层叠加：相同 padding/字体/行高，textarea 字色透明只显光标 */}
        <div className="relative font-mono text-[13px] leading-[1.75]">
          {/* 高亮显示层（浅色主题） */}
          {hasHighlight && (
            <div
              aria-hidden="true"
              className="pointer-events-none absolute inset-0 overflow-hidden
                         dark:hidden
                         [&_pre]:!m-0 [&_pre]:!bg-transparent [&_pre]:!whitespace-pre-wrap
                         [&_pre]:!break-words [&_pre]:px-4 [&_pre]:py-4
                         [&_code]:!whitespace-pre-wrap [&_code]:!break-words"
              dangerouslySetInnerHTML={{ __html: highlighted!.light }}
            />
          )}
          {/* 高亮显示层（深色主题） */}
          {hasHighlight && (
            <div
              aria-hidden="true"
              className="pointer-events-none absolute inset-0 hidden overflow-hidden
                         dark:block
                         [&_pre]:!m-0 [&_pre]:!bg-transparent [&_pre]:!whitespace-pre-wrap
                         [&_pre]:!break-words [&_pre]:px-4 [&_pre]:py-4
                         [&_code]:!whitespace-pre-wrap [&_code]:!break-words"
              dangerouslySetInnerHTML={{ __html: highlighted!.dark }}
            />
          )}

          {/* 编辑层：高亮就绪后字色透明，仅显示 caret + 选区背景 */}
          <textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            spellCheck={false}
            autoCapitalize="off"
            autoCorrect="off"
            className={`relative block w-full resize-none whitespace-pre-wrap break-words bg-transparent
                        px-4 py-4 font-mono text-[13px] leading-[1.75]
                        caret-[var(--text-primary)]
                        selection:bg-[var(--text-primary)]/20
                        ${hasHighlight ? "text-transparent" : "text-[var(--text-primary)]"}`}
            // 用内联 style 覆盖 globals.css 全局 :focus-visible 的绿色焦点环
            style={{ minHeight: 96, outline: "none", boxShadow: "none" }}
          />
        </div>

        {/* 底部提示 */}
        <div className="border-t border-[var(--code-border)] px-4 py-2">
          <span className="text-[11px] text-[var(--code-label)]">
            点击上方内容区域即可编辑 · 尖括号内为占位值，替换为实际参数后复制使用
          </span>
        </div>
      </div>
    </div>
  );
}
