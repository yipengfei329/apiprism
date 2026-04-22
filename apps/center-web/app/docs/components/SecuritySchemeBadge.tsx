"use client";

import { useState, useEffect, useRef } from "react";
import type { CanonicalOAuthFlow, CanonicalSecurityScheme } from "../lib/api";

// ── 认证类型标签 ──────────────────────────────────────────────

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

// ── 使用方式代码块 ─────────────────────────────────────────────

function UsageBlock({ name, scheme }: { name: string; scheme: CanonicalSecurityScheme }) {
  const t = scheme.type?.toLowerCase();
  const s = scheme.scheme?.toLowerCase();

  let label = "使用方式";
  let instruction: string | null = null;

  if (t === "http" && s === "bearer") {
    instruction = `Authorization: Bearer <token>${scheme.bearerFormat ? `  (${scheme.bearerFormat})` : ""}`;
  } else if (t === "http" && s === "basic") {
    instruction = "Authorization: Basic <Base64(用户名:密码)>";
  } else if (t === "apikey") {
    const p = scheme.paramName ?? name;
    if (scheme.in === "header") { label = "请求头"; instruction = `${p}: <key>`; }
    else if (scheme.in === "query") { label = "Query 参数"; instruction = `?${p}=<key>`; }
    else if (scheme.in === "cookie") { label = "Cookie"; instruction = `Cookie: ${p}=<key>`; }
  }

  if (!instruction) return null;

  return (
    <div>
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
        {label}
      </p>
      <div className="rounded-lg bg-[var(--bg-muted)] px-4 py-2.5 font-mono text-[12px] leading-relaxed text-[var(--text-primary)]">
        {instruction}
      </div>
    </div>
  );
}

// ── OAuth2 流程卡 ─────────────────────────────────────────────

const FLOW_LABELS: Record<string, string> = {
  implicit: "Implicit",
  password: "Resource Owner Password",
  clientCredentials: "Client Credentials",
  authorizationCode: "Authorization Code",
};

function OAuthFlowCard({ flow }: { flow: CanonicalOAuthFlow }) {
  const scopeEntries = Object.entries(flow.scopes ?? {});
  return (
    <div className="rounded-lg border border-[var(--border-default)] p-4">
      <p className="mb-3 text-[12px] font-semibold text-[var(--text-primary)]">
        {FLOW_LABELS[flow.flowType] ?? flow.flowType}
      </p>
      <div className="space-y-2">
        {flow.authorizationUrl && (
          <div className="flex gap-2 text-[12px]">
            <span className="w-20 shrink-0 text-[var(--text-tertiary)]">授权端点</span>
            <code className="break-all font-mono text-[11px] text-[var(--text-secondary)]">
              {flow.authorizationUrl}
            </code>
          </div>
        )}
        {flow.tokenUrl && (
          <div className="flex gap-2 text-[12px]">
            <span className="w-20 shrink-0 text-[var(--text-tertiary)]">Token 端点</span>
            <code className="break-all font-mono text-[11px] text-[var(--text-secondary)]">
              {flow.tokenUrl}
            </code>
          </div>
        )}
        {scopeEntries.length > 0 && (
          <div className="mt-3 space-y-1.5">
            <p className="text-[11px] font-semibold uppercase tracking-[0.06em] text-[var(--text-tertiary)]">
              Scopes
            </p>
            {scopeEntries.map(([scope, desc]) => (
              <div key={scope} className="flex gap-2 text-[12px]">
                <code className="shrink-0 font-mono text-[var(--accent)]">{scope}</code>
                {desc && <span className="text-[var(--text-secondary)]">{desc}</span>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

// ── 主组件 ────────────────────────────────────────────────────

type Props = {
  name: string;
  scheme: CanonicalSecurityScheme | undefined;
};

export function SecuritySchemeBadge({ name, scheme }: Props) {
  const [open, setOpen] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const el = dialogRef.current;
    if (!el) return;
    if (open) el.showModal();
    else if (el.open) el.close();
  }, [open]);

  // 监听原生 close 事件（ESC 键触发）
  useEffect(() => {
    const el = dialogRef.current;
    if (!el) return;
    const onClose = () => setOpen(false);
    el.addEventListener("close", onClose);
    return () => el.removeEventListener("close", onClose);
  }, []);

  const hasDetails = Boolean(scheme);

  return (
    <>
      <button
        type="button"
        onClick={() => hasDetails && setOpen(true)}
        title={hasDetails ? `查看 ${name} 认证方案详情` : name}
        className={[
          "inline-flex items-center gap-1.5 rounded-xl px-4 py-2 text-[13px] transition-colors",
          "bg-[var(--bg-subtle)] text-[var(--text-secondary)]",
          hasDetails
            ? "cursor-pointer hover:bg-[var(--bg-muted)] hover:text-[var(--text-primary)]"
            : "cursor-default",
        ].join(" ")}
      >
        {/* Lock icon */}
        <svg className="h-3.5 w-3.5 shrink-0" viewBox="0 0 16 16" fill="currentColor">
          <path fillRule="evenodd" d="M8 1a3.5 3.5 0 00-3.5 3.5V7A1.5 1.5 0 003 8.5v5A1.5 1.5 0 004.5 15h7a1.5 1.5 0 001.5-1.5v-5A1.5 1.5 0 0011.5 7V4.5A3.5 3.5 0 008 1zm2 6V4.5a2 2 0 10-4 0V7h4z" clipRule="evenodd" />
        </svg>
        {name}
        {/* Info icon — 有详情时显示 */}
        {hasDetails && (
          <svg className="h-3 w-3 shrink-0 opacity-40" viewBox="0 0 16 16" fill="currentColor">
            <path fillRule="evenodd" d="M8 15A7 7 0 108 1a7 7 0 000 14zm1-9a1 1 0 11-2 0 1 1 0 012 0zM7 7a1 1 0 000 2v3a1 1 0 102 0V9a1 1 0 00-1-1H7z" clipRule="evenodd" />
          </svg>
        )}
      </button>

      {/* 弹窗 */}
      <dialog
        ref={dialogRef}
        className="m-auto w-full max-w-[480px] overflow-hidden rounded-2xl border border-[var(--border-default)] bg-[var(--bg-surface)] p-0 shadow-2xl backdrop:bg-black/55"
        onClick={(e) => { if (e.target === e.currentTarget) setOpen(false); }}
      >
        {scheme && (
          <>
            {/* 弹窗头部 */}
            <div className="flex items-center justify-between border-b border-[var(--border-default)] px-6 py-4">
              <div className="flex items-center gap-2.5">
                <svg className="h-4 w-4 text-[var(--accent)]" viewBox="0 0 16 16" fill="currentColor">
                  <path fillRule="evenodd" d="M8 1a3.5 3.5 0 00-3.5 3.5V7A1.5 1.5 0 003 8.5v5A1.5 1.5 0 004.5 15h7a1.5 1.5 0 001.5-1.5v-5A1.5 1.5 0 0011.5 7V4.5A3.5 3.5 0 008 1zm2 6V4.5a2 2 0 10-4 0V7h4z" clipRule="evenodd" />
                </svg>
                <span className="font-semibold text-[var(--text-primary)]">{name}</span>
                <span className="rounded-md bg-[var(--accent-bg)] px-2 py-0.5 text-[11px] font-semibold text-[var(--accent)]">
                  {schemeTypeLabel(scheme)}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setOpen(false)}
                className="flex h-7 w-7 items-center justify-center rounded-lg text-[var(--text-tertiary)] transition-colors hover:bg-[var(--bg-muted)] hover:text-[var(--text-primary)]"
              >
                <svg viewBox="0 0 14 14" className="h-3.5 w-3.5" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round">
                  <path d="M1 1l12 12M13 1L1 13" />
                </svg>
              </button>
            </div>

            {/* 弹窗内容 */}
            <div className="space-y-5 px-6 py-5">
              <UsageBlock name={name} scheme={scheme} />

              {scheme.description && (
                <div>
                  <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
                    说明
                  </p>
                  <p className="text-[13px] leading-[1.65] text-[var(--text-secondary)]">
                    {scheme.description}
                  </p>
                </div>
              )}

              {scheme.oauthFlows && scheme.oauthFlows.length > 0 && (
                <div>
                  <p className="mb-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
                    OAuth2 授权流程
                  </p>
                  <div className="space-y-3">
                    {scheme.oauthFlows.map((flow) => (
                      <OAuthFlowCard flow={flow} key={flow.flowType} />
                    ))}
                  </div>
                </div>
              )}

              {scheme.openIdConnectUrl && (
                <div>
                  <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
                    发现端点
                  </p>
                  <code className="block break-all rounded-lg bg-[var(--bg-muted)] px-4 py-2.5 font-mono text-[12px] text-[var(--text-primary)]">
                    {scheme.openIdConnectUrl}
                  </code>
                </div>
              )}
            </div>
          </>
        )}
      </dialog>
    </>
  );
}
