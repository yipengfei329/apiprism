import type { CanonicalServiceSnapshot } from "../lib/api";

type Props = { snapshot: CanonicalServiceSnapshot };

const METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

/**
 * 服务统计：与接口详情页的 facts strip 统一，用细分割承载扫描信息。
 */
export function ServiceStats({ snapshot }: Props) {
  const allOps = snapshot.groups.flatMap((g) => g.operations);
  const total = allOps.length;
  if (total === 0) return null;

  const groupCount = snapshot.groups.length;
  const secured = allOps.filter((op) => op.securityRequirements.length > 0).length;
  const securedPct = Math.round((secured / total) * 100);

  // method 分布（保留出现过的，按 OpenAPI 标准顺序）
  const methodCounts: Record<string, number> = {};
  for (const op of allOps) {
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
  ];

  return (
    <section className="docs-facts-strip">
      <div className="grid grid-cols-2 gap-0 md:grid-cols-[1fr_1fr_1fr_2fr]">
        <Stat value={total} label="接口" />
        <Stat value={groupCount} label="分组" />
        <Stat value={`${securedPct}%`} label="认证覆盖" />
        <div className="min-w-0 px-4 py-3 first:pl-0 md:border-l md:border-[var(--border-subtle)]">
          <div className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)]">方法分布</div>
          <div className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1.5 font-mono text-[12.5px]">
            {methods.map(({ method, count }) => {
              const m = method.toLowerCase();
              return (
                <span key={method} className="inline-flex items-baseline gap-1.5">
                  <span className="font-semibold tabular-nums text-[var(--text-secondary)]">
                    {count}
                  </span>
                  <span
                    className="text-[11px] font-semibold tracking-wider"
                    style={{ color: `var(--method-${m}-text, var(--text-tertiary))` }}
                  >
                    {method}
                  </span>
                </span>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}

function Stat({ value, label }: { value: number | string; label: string }) {
  return (
    <div className="min-w-0 px-4 py-3 first:pl-0 md:border-l md:border-[var(--border-subtle)] md:first:border-l-0 md:first:pl-0">
      <div className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)]">{label}</div>
      <span className="mt-1.5 block text-[13px] font-semibold leading-snug tabular-nums text-[var(--text-secondary)]">
        {value}
      </span>
    </div>
  );
}
