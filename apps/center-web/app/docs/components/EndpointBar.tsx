"use client";

import { useCallback, useState } from "react";
import { Check, Copy } from "@phosphor-icons/react";
import { MethodBadge } from "./MethodBadge";

interface EndpointBarProps {
  method: string;
  path: string;
  /** 可选的内容类型行（请求/响应 ContentType） */
  meta?: { label: string; value: string }[];
}

/**
 * 接口详情页头部的「端点物件」：
 *   [GET] /api/users/{id}                              [⌘C]
 *   ─────────────────────────────────────────────────────
 *   请求 application/json   响应 application/json
 *
 * 整块作为可复制单元，hover 时右侧显出 copy 按钮（路径含基础协议前的 path）。
 * Stripe Docs / Mintlify 风的「endpoint object」处理。
 */
export function EndpointBar({ method, path, meta = [] }: EndpointBarProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(path);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1200);
    } catch {
      // 复制失败静默
    }
  }, [path]);

  return (
    <div className="docs-copy-target group/endpoint mt-7 overflow-hidden rounded-xl border border-[var(--border-default)] bg-[var(--bg-subtle)]/55">
      <div className="flex min-w-0 items-center gap-3 px-5 py-4">
        <MethodBadge method={method} size="lg" />
        <code className="min-w-0 flex-1 overflow-x-auto whitespace-nowrap font-mono text-[15px] font-medium leading-[1.5] text-[var(--text-primary)] md:text-[16px]">
          {path}
        </code>
        <button
          type="button"
          onClick={handleCopy}
          className="docs-copy-btn shrink-0 inline-flex h-7 w-7 items-center justify-center rounded-md text-[var(--text-tertiary)] hover:bg-[var(--bg-muted)] hover:text-[var(--text-primary)]"
          aria-label="复制路径"
          title={copied ? "已复制" : "复制路径"}
          data-copied={copied || undefined}
        >
          {copied ? (
            <Check size={14} weight="bold" className="text-[var(--accent)]" />
          ) : (
            <Copy size={14} weight="regular" />
          )}
        </button>
      </div>
      {meta.length > 0 && (
        <div className="flex flex-wrap items-center gap-x-5 gap-y-1.5 border-t border-[var(--border-subtle)] px-5 py-2.5">
          {meta.map((m) => (
            <span key={m.label} className="inline-flex items-center gap-1.5 text-[12px]">
              <span className="text-[var(--text-tertiary)]">{m.label}</span>
              <span className="font-mono text-[var(--text-secondary)]">{m.value}</span>
            </span>
          ))}
        </div>
      )}
    </div>
  );
}
