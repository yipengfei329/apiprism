import type { CanonicalServiceSnapshot } from "../lib/api";

type Props = { snapshot: CanonicalServiceSnapshot };

const METHOD_ORDER = ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"];

/**
 * 服务统计：左侧三个安静大数字 + 右侧方法分布紧凑列表
 *
 * 思路：
 *  - 数字主导，靠字号承担视觉重量
 *  - 方法分布回归——程序员真正在意 GET/POST 的比例，不能省
 *  - 不画条形图（dashboardy），用 mono `4 · GET` 紧凑列出，按 method 颜色着色
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
    <section className="mb-14 grid gap-10 sm:grid-cols-[auto_1fr] sm:items-end sm:gap-14">
      {/* ── 左：三个安静大数字 ── */}
      <div className="flex flex-wrap items-end gap-x-10 gap-y-6">
        <Stat value={total} label="接口" />
        <Stat value={groupCount} label="分组" />
        {securedPct > 0 && <Stat value={`${securedPct}%`} label="认证覆盖" />}
      </div>

      {/* ── 右：方法分布紧凑列表 ── */}
      <div className="min-w-0 sm:pb-1">
        <div className="mb-3 docs-eyebrow">方法分布</div>
        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 font-mono text-[12.5px]">
          {methods.map(({ method, count }) => {
            const m = method.toLowerCase();
            return (
              <span key={method} className="inline-flex items-baseline gap-1.5">
                <span
                  className="font-semibold tabular-nums text-[var(--text-primary)]"
                >
                  {count}
                </span>
                <span
                  className="text-[11.5px] font-medium tracking-wider"
                  style={{ color: `var(--method-${m}-text, var(--text-tertiary))` }}
                >
                  {method}
                </span>
              </span>
            );
          })}
        </div>
      </div>
    </section>
  );
}

function Stat({ value, label }: { value: number | string; label: string }) {
  return (
    <div className="flex flex-col gap-2">
      <span
        className="text-[clamp(2rem,3.4vw,2.7rem)] font-semibold leading-none tabular-nums text-[var(--text-primary)]"
        style={{ letterSpacing: "-0.035em" }}
      >
        {value}
      </span>
      <span className="text-[12.5px] text-[var(--text-tertiary)]">{label}</span>
    </div>
  );
}
