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
  const avgPerGroup = groupCount > 0 ? (total / groupCount).toFixed(1) : "0";

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

  // SVG 环形进度（认证覆盖率）
  const R = 22;
  const C = +(2 * Math.PI * R).toFixed(2);
  const dash = +((securedPct / 100) * C).toFixed(2);

  return (
    <section className="mb-14">
      {/* 三张统计卡片 */}
      <div className="mb-3 grid grid-cols-3 gap-3">

        {/* 接口总数 — accent 强调 */}
        <div
          className="rounded-xl p-5 v-card-full v-slide-up"
          style={{
            background: "var(--accent-bg)",
            borderColor: "var(--accent-border)",
          }}
        >
          <p
            className="text-[11px] font-semibold uppercase tracking-[0.08em]"
            style={{ color: "var(--accent)" }}
          >
            接口总数
          </p>
          <p
            className="mt-2 font-mono font-bold leading-none"
            style={{
              fontSize: "clamp(2rem, 4vw, 2.8rem)",
              letterSpacing: "-0.04em",
              color: "var(--accent)",
            }}
          >
            {total}
          </p>
          <p className="mt-2 text-[12px]" style={{ color: "var(--accent)", opacity: 0.65 }}>
            endpoints
          </p>
        </div>

        {/* 接口分组 */}
        <div className="rounded-xl bg-[var(--bg-surface)] p-5 v-card-full v-slide-up v-delay-1">
          <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
            接口分组
          </p>
          <p
            className="mt-2 font-mono font-bold leading-none text-[var(--text-primary)]"
            style={{
              fontSize: "clamp(2rem, 4vw, 2.8rem)",
              letterSpacing: "-0.04em",
            }}
          >
            {groupCount}
          </p>
          <p className="mt-2 text-[12px] text-[var(--text-tertiary)]">
            均 {avgPerGroup} 个/组
          </p>
        </div>

        {/* 认证覆盖率 — SVG 环形 */}
        <div className="rounded-xl bg-[var(--bg-surface)] p-5 v-card-full v-slide-up v-delay-2">
          <p className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
            认证覆盖
          </p>
          <div className="mt-2 flex items-center gap-3">
            <svg
              width="52"
              height="52"
              viewBox="0 0 52 52"
              style={{ transform: "rotate(-90deg)", flexShrink: 0 }}
            >
              {/* 背景圆轨道 */}
              <circle
                cx="26"
                cy="26"
                r={R}
                fill="none"
                strokeWidth="4.5"
                stroke="var(--bg-muted)"
              />
              {/* 进度弧 */}
              {securedPct > 0 && (
                <circle
                  cx="26"
                  cy="26"
                  r={R}
                  fill="none"
                  strokeWidth="4.5"
                  stroke="var(--accent)"
                  strokeLinecap="round"
                  strokeDasharray={`${dash} ${C}`}
                />
              )}
            </svg>
            <div>
              <p
                className="font-mono font-bold leading-none text-[var(--text-primary)]"
                style={{ fontSize: "1.65rem", letterSpacing: "-0.04em" }}
              >
                {securedPct}%
              </p>
              <p className="mt-1 font-mono text-[11px] text-[var(--text-tertiary)]">
                {secured}/{total}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* HTTP 方法分布 */}
      {methods.length > 0 && (
        <div className="rounded-xl bg-[var(--bg-surface)] p-5 v-card-full v-slide-up v-delay-3">
          <p className="mb-4 text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
            HTTP 方法分布
          </p>

          {/* 分段进度条 */}
          <div className="mb-5 flex h-[6px] overflow-hidden rounded-full gap-[2px]">
            {methods.map(({ method, count }) => (
              <div
                key={method}
                title={`${method}: ${count}`}
                style={{
                  flex: count,
                  backgroundColor: `var(--method-${method.toLowerCase()}-text, var(--text-tertiary))`,
                  opacity: 0.8,
                  borderRadius: "9999px",
                }}
              />
            ))}
          </div>

          {/* 图例 Pills */}
          <div className="flex flex-wrap gap-2">
            {methods.map(({ method, count }) => {
              const pct = Math.round((count / total) * 100);
              const m = method.toLowerCase();
              return (
                <div
                  key={method}
                  className="flex items-center gap-2 rounded-lg px-2.5 py-1.5"
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
                  <span className="font-mono text-[13px] font-bold text-[var(--text-primary)]">
                    {count}
                  </span>
                  <span className="font-mono text-[11px] text-[var(--text-tertiary)]">
                    {pct}%
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}
    </section>
  );
}
