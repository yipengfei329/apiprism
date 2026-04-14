"use client";

import { useCallback, useState } from "react";
import { Robot, Link as LinkIcon, Check } from "@phosphor-icons/react";

interface AgentDocLinkProps {
  /** apidocs.md 的前端路径，如 /svc/env/apidocs.md */
  path: string;
}

export function AgentDocLink({ path }: AgentDocLinkProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    const url = `${window.location.origin}${path}`;
    navigator.clipboard.writeText(url).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
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
        Agent Doc
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
          <Check size={13} weight="bold" className="text-green-600" />
        ) : (
          <LinkIcon size={13} />
        )}
      </button>
    </span>
  );
}
