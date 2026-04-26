import type { ReactNode } from "react";

interface SectionLabelProps {
  children: ReactNode;
  /** 右侧可选计数（小灰色 tabular-nums） */
  counter?: ReactNode;
  /** 是否显示左侧 2px accent 短杠（Linear 风），默认 true */
  bar?: boolean;
  className?: string;
}

/**
 * Section eyebrow 标签：
 *   ▎ GROUPS · 6
 *
 * 单行小字 + 可选左侧 accent 短杠（默认开启）。靠 letter-spacing + 字重做质感，
 * 比纯小灰文本多一个识别点，让读者扫到章节边界时眼睛能停一下。
 */
export function SectionLabel({
  children,
  counter,
  bar = true,
  className = "",
}: SectionLabelProps) {
  const wrapperClass = bar
    ? "docs-eyebrow-bar items-center"
    : "inline-flex items-baseline gap-2";

  return (
    <div className={`${wrapperClass} ${className}`.trim()}>
      <span className="docs-eyebrow">{children}</span>
      {counter !== undefined && (
        <span className="text-[12px] font-medium tabular-nums text-[var(--text-quaternary)]">
          {counter}
        </span>
      )}
    </div>
  );
}
