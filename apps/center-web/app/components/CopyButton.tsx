"use client";

import { useCallback, useState } from "react";
import { Copy, Check } from "@phosphor-icons/react";

interface CopyButtonProps {
  text: string;
  /** 视觉风格，默认 light */
  variant?: "light" | "dark";
}

export function CopyButton({ text, variant = "light" }: CopyButtonProps) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(() => {
    navigator.clipboard.writeText(text).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    });
  }, [text]);

  const colors =
    variant === "dark"
      ? "text-white/40 hover:text-white/70 hover:bg-white/[0.08]"
      : "text-v-gray-400 hover:text-v-gray-600 hover:bg-v-gray-50";

  return (
    <button
      type="button"
      onClick={handleCopy}
      className={`flex items-center gap-1 rounded-md px-2 py-1 text-[11px] font-medium
                  transition-colors duration-150 cursor-pointer select-none ${colors}`}
      title="复制"
    >
      {copied ? (
        <>
          <Check size={13} weight="bold" className="text-[var(--accent)]" />
          <span className="text-[var(--accent)]">已复制</span>
        </>
      ) : (
        <>
          <Copy size={13} />
          <span>复制</span>
        </>
      )}
    </button>
  );
}
