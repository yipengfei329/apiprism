import type { CanonicalSecurityScheme } from "../lib/api";
import { SecuritySchemeBadge } from "./SecuritySchemeBadge";

type Props = {
  securitySchemes: Record<string, CanonicalSecurityScheme>;
};

function schemeTypeLabel(scheme: CanonicalSecurityScheme): string {
  const t = scheme.type?.toLowerCase();
  if (t === "http") {
    const s = scheme.scheme?.toLowerCase();
    if (s === "bearer") return "HTTP Bearer";
    if (s === "basic") return "HTTP Basic";
    return "HTTP";
  }
  if (t === "apikey") return "API Key";
  if (t === "oauth2") return "OAuth 2.0";
  if (t === "openidconnect") return "OpenID Connect";
  if (t === "mutualtls") return "Mutual TLS";
  return scheme.type ?? "Unknown";
}

function getUsageHint(name: string, scheme: CanonicalSecurityScheme): string | null {
  const t = scheme.type?.toLowerCase();
  if (t === "http") {
    const s = scheme.scheme?.toLowerCase();
    if (s === "bearer") return `Authorization: Bearer <token>${scheme.bearerFormat ? `  (${scheme.bearerFormat})` : ""}`;
    if (s === "basic") return "Authorization: Basic <Base64>";
  }
  if (t === "apikey") {
    const p = scheme.paramName ?? name;
    if (scheme.in === "header") return `${p}: <key>`;
    if (scheme.in === "query") return `?${p}=<key>`;
    if (scheme.in === "cookie") return `Cookie: ${p}=<key>`;
  }
  if (t === "oauth2") return "OAuth2 Bearer Token";
  if (t === "openidconnect") return "OpenID Connect Token";
  return null;
}

export function ServiceSecuritySchemes({ securitySchemes }: Props) {
  const entries = Object.entries(securitySchemes ?? {});
  if (entries.length === 0) return null;

  return (
    <section className="mb-14">
      <p className="mb-4 text-[12px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
        认证方式
      </p>
      <div className="grid gap-3 sm:grid-cols-2">
        {entries.map(([name, scheme]) => {
          const label = schemeTypeLabel(scheme);
          const usage = getUsageHint(name, scheme);
          return (
            <div
              key={name}
              className="rounded-xl bg-[var(--bg-surface)] p-5 v-card-full"
            >
              {/* 名称 + 类型徽章 */}
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-2">
                  <svg className="h-3.5 w-3.5 shrink-0 text-[var(--accent)]" viewBox="0 0 16 16" fill="currentColor">
                    <path fillRule="evenodd" d="M8 1a3.5 3.5 0 00-3.5 3.5V7A1.5 1.5 0 003 8.5v5A1.5 1.5 0 004.5 15h7a1.5 1.5 0 001.5-1.5v-5A1.5 1.5 0 0011.5 7V4.5A3.5 3.5 0 008 1zm2 6V4.5a2 2 0 10-4 0V7h4z" clipRule="evenodd" />
                  </svg>
                  <span className="font-semibold text-[var(--text-primary)]">{name}</span>
                </div>
                <span className="shrink-0 rounded-md bg-[var(--accent-bg)] px-2 py-0.5 text-[11px] font-semibold text-[var(--accent)]">
                  {label}
                </span>
              </div>

              {/* 使用方式代码 */}
              {usage && (
                <div className="mt-3 rounded-lg bg-[var(--bg-muted)] px-3.5 py-2 font-mono text-[12px] text-[var(--text-secondary)]">
                  {usage}
                </div>
              )}

              {/* OAuth2 流程标签 */}
              {scheme.type?.toLowerCase() === "oauth2" && scheme.oauthFlows && scheme.oauthFlows.length > 0 && (
                <div className="mt-2.5 flex flex-wrap gap-1.5">
                  {scheme.oauthFlows.map((flow) => (
                    <span
                      key={flow.flowType}
                      className="rounded-md bg-[var(--bg-subtle)] px-2 py-0.5 font-mono text-[11px] text-[var(--text-tertiary)]"
                    >
                      {flow.flowType}
                    </span>
                  ))}
                </div>
              )}

              {/* 说明文字 */}
              {scheme.description && (
                <p className="mt-3 text-[12px] leading-[1.65] text-[var(--text-tertiary)]">
                  {scheme.description}
                </p>
              )}

              {/* 详情按钮 */}
              <div className="mt-4">
                <SecuritySchemeBadge name={name} scheme={scheme} />
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
