"use client";

import { useCallback, useState } from "react";
import { Check, Copy } from "@phosphor-icons/react";

interface CopyableMonoProps {
  /** 显示并复制的内容 */
  value: string;
  /** 视觉变体：默认 inline 紧凑、lg 用于服务地址等主信息 */
  size?: "sm" | "lg";
  /** 容器额外样式 */
  className?: string;
  /** 标题（aria-label / title） */
  ariaLabel?: string;
}

/**
 * 文档中心的「mono signature move」：路径 / ID / URL 等可复制的代码片段。
 *  - 默认 hairline 边框 + 极淡背景
 *  - hover 时右侧出现 copy 按钮，键盘 focus 也显示
 *  - 复制后 1.2s 内显示对勾
 *
 * 这是程序员一眼能识别的「代码物件」——值得被点、值得被复制。
 */
export function CopyableMono({
  value,
  size = "sm",
  className = "",
  ariaLabel,
}: CopyableMonoProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(
    async (e: React.MouseEvent) => {
      // 阻止冒泡到外层 Link / Button，避免误触跳转
      e.preventDefault();
      e.stopPropagation();
      try {
        await navigator.clipboard.writeText(value);
        setCopied(true);
        window.setTimeout(() => setCopied(false), 1200);
      } catch {
        // 复制失败静默——浏览器在非 https 环境会拒绝写入
      }
    },
    [value],
  );

  const chipClass =
    size === "lg" ? "docs-mono-chip docs-mono-chip-lg" : "docs-mono-chip";

  return (
    <span
      className={`docs-copy-target group/copy ${chipClass} ${className}`.trim()}
    >
      <span className="truncate">{value}</span>
      <button
        type="button"
        onClick={handleCopy}
        className="docs-copy-btn ml-2 -mr-1 inline-flex h-5 w-5 items-center justify-center rounded text-[var(--text-tertiary)] hover:bg-[var(--bg-muted)] hover:text-[var(--text-primary)]"
        aria-label={ariaLabel ?? `复制 ${value}`}
        title={copied ? "已复制" : "复制"}
        data-copied={copied || undefined}
      >
        {copied ? (
          <Check size={12} weight="bold" className="text-[var(--accent)]" />
        ) : (
          <Copy size={12} weight="regular" />
        )}
      </button>
    </span>
  );
}
