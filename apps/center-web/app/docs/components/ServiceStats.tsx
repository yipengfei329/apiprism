import type { CanonicalServiceSnapshot } from "../lib/api";

type Props = { snapshot: CanonicalServiceSnapshot };

const METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

export function ServiceStats({ snapshot }: Props) {
  const allOps = snapshot.groups.flatMap((g) => g.operations);
  const total = allOps.length;
  if (total === 0) return null;

  const groupCount = snapshot.groups.length;
  const secured = allOps.filter((op) => op.securityRequirements.length > 0).length;
  const securedPct = Math.round((secured / total) * 100);

  // HTTP 方法分布统计
  const methodCounts: Record<string, number> = {};
  for (const op of allOps) {
    const m = op.method.toUpperCase();
    methodCounts[m] = (methodCounts[m] ?? 0) + 1;
  }
  const methods = [
    ...METHOD_ORDER.filter((m) => methodCounts[m]).map((m) => ({ method: m, count: methodCounts[m] })),
    ...Object.entries(methodCounts)
      .filter(([m]) => !METHOD_ORDER.includes(m))
      .sort((a, b) => b[1] - a[1])
      .map(([method, count]) => ({ method, count })),
  ];

  return (
    <section className="mb-10">
      <div className="flex flex-wrap items-center gap-x-1.5 gap-y-2">
        {/* 接口总数 — accent 高亮 */}
        <span className="rounded-full border border-[var(--accent-border)] bg-[var(--accent-bg)] px-3 py-1 font-mono text-[13px] font-bold text-[var(--accent)]">
          {total} 接口
        </span>

        <span className="select-none text-[12px] text-[var(--text-quaternary)]">·</span>

        {/* 分组数 */}
        <span className="rounded-full border border-[var(--border-default)] bg-[var(--bg-subtle)] px-3 py-1 font-mono text-[12px] font-medium text-[var(--text-secondary)]">
          {groupCount} 分组
        </span>

        {securedPct > 0 && (
          <>
            <span className="select-none text-[12px] text-[var(--text-quaternary)]">·</span>
            {/* 认证覆盖率 */}
            <span className="rounded-full border border-[var(--border-default)] bg-[var(--bg-subtle)] px-3 py-1 font-mono text-[12px] font-medium text-[var(--text-secondary)]">
              {securedPct}% 认证
            </span>
          </>
        )}

        {/* HTTP 方法 pills — 复用现有 CSS var */}
        {methods.map(({ method, count }) => {
          const m = method.toLowerCase();
          return (
            <div
              key={method}
              className="flex items-center gap-1.5 rounded-lg px-2.5 py-1"
              style={{
                backgroundColor: `var(--method-${m}-bg, var(--bg-subtle))`,
                border: `1px solid var(--method-${m}-border, var(--border-default))`,
              }}
            >
              <span
                className="font-mono text-[12px] font-semibold"
                style={{ color: `var(--method-${m}-text, var(--text-secondary))` }}
              >
                {method}
              </span>
              <span className="font-mono text-[12px] font-bold text-[var(--text-primary)]">
                {count}
              </span>
            </div>
          );
        })}
      </div>
    </section>
  );
}
