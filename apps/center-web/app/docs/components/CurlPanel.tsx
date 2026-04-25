"use client";

import { useState, useEffect, useRef } from "react";
import { ArrowCounterClockwise, Terminal } from "@phosphor-icons/react";
import { CopyButton } from "@/app/components/CopyButton";
import { type CanonicalOperation, type CanonicalSecurityScheme } from "../lib/api";
import { generateExample } from "./generateExample";

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
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // textarea 自动伸展
  useEffect(() => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = el.scrollHeight + "px";
  }, [value]);

  const isDirty = value !== initialCommand;

  return (
    <div className="space-y-3">
      {/* 区块标题 */}
      <div className="flex items-center gap-2">
        <Terminal size={14} className="text-v-gray-400" />
        <span className="text-[13px] font-medium text-v-gray-500">cURL 示范</span>
        <span className="text-[11px] text-v-gray-300">· 填入真实参数后复制执行</span>
      </div>

      {/* 代码区块 */}
      <div className="overflow-hidden rounded-xl border border-[var(--border-default)] bg-[#282c34]">
        {/* 顶栏 */}
        <div className="flex items-center justify-between border-b border-white/[0.08] px-4 py-2">
          <span className="text-[11px] font-medium tracking-wide text-white/40">Shell</span>
          <div className="flex items-center gap-1">
            {isDirty && (
              <button
                type="button"
                onClick={() => setValue(initialCommand)}
                className="flex cursor-pointer select-none items-center gap-1 rounded-md px-2 py-1
                           text-[11px] font-medium text-white/40 transition-colors duration-150
                           hover:bg-white/[0.08] hover:text-white/70"
                title="重置为生成示例"
              >
                <ArrowCounterClockwise size={12} />
                <span>重置</span>
              </button>
            )}
            <CopyButton text={value} variant="dark" />
          </div>
        </div>

        {/* 可编辑文本区 */}
        <textarea
          ref={textareaRef}
          value={value}
          onChange={(e) => setValue(e.target.value)}
          spellCheck={false}
          autoCapitalize="off"
          autoCorrect="off"
          className="block w-full resize-none bg-transparent px-4 py-4 font-mono
                     text-[13px] leading-[1.75] text-[#abb2bf] outline-none"
          style={{ minHeight: 96 }}
        />

        {/* 底部提示 */}
        <div className="border-t border-white/[0.04] px-4 py-2">
          <span className="text-[11px] text-white/20">
            点击上方内容区域即可编辑 · 尖括号内为占位值，替换为实际参数后复制使用
          </span>
        </div>
      </div>
    </div>
  );
}
