import type { ReactNode } from "react";
import Link from "next/link";
import { HtmlText } from "../../components/HtmlText";
import { AgentDocLink } from "../../components/AgentDocLink";
import type { CanonicalGroup } from "../../lib/api";

interface GroupCardProps {
  group: CanonicalGroup;
  /** 卡片主链接：进入分组详情 */
  href: string;
  /** Agent 文档 markdown 路径，留空时不渲染按钮 */
  agentDocPath?: string;
  /** 拖拽手柄槽位；非排序场景（如查看历史版本）传 undefined 即可 */
  dragHandle?: ReactNode;
}

const METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

/**
 * 分组卡片：Linear/Vercel 风「可点击物件」感
 *  - 圆角 xl + 1px hairline，hover 边框深一档
 *  - 底部加 method 分布迷你统计行——程序员真正在意的信息
 *  - 描述区预留两行高度保证多卡对齐
 */
export function GroupCard({ group, href, agentDocPath, dragHandle }: GroupCardProps) {
  // 方法分布（最多展示前 4 种，避免拥挤）
  const methodCounts: Record<string, number> = {};
  for (const op of group.operations) {
    const m = op.method.toUpperCase();
    methodCounts[m] = (methodCounts[m] ?? 0) + 1;
  }
  const methods = [
    ...METHOD_ORDER.filter((m) => methodCounts[m]).map((m) => ({
      method: m,
      count: methodCounts[m],
    })),
    ...Object.entries(methodCounts)
      .filter(([m]) => !METHOD_ORDER.includes(m))
      .sort((a, b) => b[1] - a[1])
      .map(([method, count]) => ({ method, count })),
  ].slice(0, 4);

  return (
    <div className="docs-quiet-card group/card relative flex min-h-[152px] flex-col rounded-xl bg-[var(--bg-surface)] p-5">
      <Link
        href={href}
        aria-label={group.name}
        className="absolute inset-0 z-0 rounded-xl"
      />

      {dragHandle}

      <div className="pointer-events-none relative z-[1] pr-7">
        <h3
          className="line-clamp-1 text-[16px] font-semibold text-[var(--text-primary)]"
          style={{ letterSpacing: "-0.01em" }}
        >
          {group.name}
        </h3>
        <div className="mt-1.5 min-h-[2.5em]">
          {group.description && (
            <HtmlText
              as="p"
              text={group.description}
              className="line-clamp-2 text-[13px] leading-[1.55] text-[var(--text-secondary)]"
            />
          )}
        </div>
      </div>

      <div className="relative z-[1] mt-auto flex items-center justify-between gap-3 pt-4">
        <div className="pointer-events-none flex min-w-0 flex-wrap items-baseline gap-x-3 gap-y-1 text-[12px] text-[var(--text-tertiary)]">
          <span>
            <span className="font-semibold tabular-nums text-[var(--text-secondary)]">
              {group.operations.length}
            </span>{" "}
            个接口
          </span>
          {methods.length > 0 && (
            <span className="flex flex-wrap items-baseline gap-x-2.5 font-mono text-[11.5px]">
              {methods.map(({ method, count }) => {
                const m = method.toLowerCase();
                return (
                  <span key={method} className="inline-flex items-baseline gap-1">
                    <span className="tabular-nums text-[var(--text-secondary)]">
                      {count}
                    </span>
                    <span
                      className="font-semibold tracking-wider"
                      style={{
                        color: `var(--method-${m}-text, var(--text-tertiary))`,
                      }}
                    >
                      {method}
                    </span>
                  </span>
                );
              })}
            </span>
          )}
        </div>
        {agentDocPath && (
          <div className="pointer-events-auto shrink-0">
            <AgentDocLink path={agentDocPath} />
          </div>
        )}
      </div>
    </div>
  );
}
