"use client";

import { useCallback, useState } from "react";
import { Robot, Copy, Check } from "@phosphor-icons/react";

interface AgentDocLinkProps {
  /** apidocs.md 的前端路径，如 /svc/env/apidocs.md */
  path: string;
}

export function AgentDocLink({ path }: AgentDocLinkProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    const url = `${window.location.origin}${path}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // navigator.clipboard 不可用时（非安全上下文、权限被拒等）的降级方案
      const el = document.createElement("textarea");
      el.value = url;
      el.style.cssText = "position:fixed;opacity:0;pointer-events:none";
      document.body.appendChild(el);
      el.focus();
      el.select();
      try {
        document.execCommand("copy");
        setCopied(true);
        setTimeout(() => setCopied(false), 1500);
      } finally {
        document.body.removeChild(el);
      }
    }
  }, [path]);

  return (
    <span className="inline-flex items-center overflow-hidden rounded-lg v-ring-light text-[12px] font-medium shrink-0">
      {/* 左侧：打开链接 */}
      <a
        href={path}
        target="_blank"
        rel="noreferrer"
        className="inline-flex items-center gap-1.5 px-2.5 py-1.5 text-v-gray-400 transition-colors hover:bg-v-gray-50 hover:text-v-gray-600"
      >
        <Robot size={13} weight="duotone" />
        Agent 文档
      </a>
      {/* 分隔线 */}
      <span className="h-4 w-px bg-v-gray-100" />
      {/* 右侧：复制链接 */}
      <button
        type="button"
        onClick={handleCopy}
        className="inline-flex cursor-pointer items-center px-2 py-1.5 text-v-gray-400 transition-colors hover:bg-v-gray-50 hover:text-v-gray-600"
        title="复制链接"
      >
        {copied ? (
          <Check size={13} weight="bold" className="text-[var(--accent)]" />
        ) : (
          <Copy size={13} />
        )}
      </button>
    </span>
  );
}
