"use client";

import { useState, type ReactNode } from "react";
import {
  BookOpen,
  Bug,
} from "@phosphor-icons/react";
import { CanonicalOperation } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { EndpointBar } from "./EndpointBar";

type TabKey = "doc" | "debug";

interface TabDef {
  key: TabKey;
  label: string;
  icon: typeof BookOpen;
}

const tabs: TabDef[] = [
  { key: "doc", label: "文档", icon: BookOpen },
  { key: "debug", label: "调试测试", icon: Bug },
];

/**
 * 接口概览摘要：用细分割线组织，不再额外堆卡片，让 endpoint object 成为视觉主角。
 */
function OperationFactsStrip({ op }: { op: CanonicalOperation }) {
  const okCount = op.responses?.filter((r) => {
    const code = parseInt(r.statusCode, 10);
    return code >= 200 && code < 300;
  }).length ?? 0;
  const errorCount = (op.responses?.length ?? 0) - okCount;

  const facts: Array<{ label: string; value: ReactNode }> = [
    {
      label: "认证",
      value: op.securityRequirements && op.securityRequirements.length > 0
        ? (
          <span className="inline-flex items-center gap-1.5">
            <span className="docs-status-dot" style={{ color: "var(--accent)" }} aria-hidden />
            需认证
          </span>
        )
        : "未声明",
    },
    {
      label: "请求体",
      value: op.requestBody?.contentType ? (
        <span className="font-mono">{op.requestBody.contentType}</span>
      ) : "无",
    },
    {
      label: "响应",
      value: (
        <span className="tabular-nums">
          {op.responses?.length ?? 0} 个
          {errorCount > 0 && (
            <span className="text-[var(--text-quaternary)]">
              {" "}({okCount} 成功 + {errorCount} 错误)
            </span>
          )}
        </span>
      ),
    },
    {
      label: "参数",
      value: <span className="tabular-nums">{op.parameters?.length ?? 0} 个</span>,
    },
  ];

  return (
    <div className="docs-facts-strip mt-7">
      <dl className="grid grid-cols-2 gap-0 md:grid-cols-4">
        {facts.map((fact) => (
          <div key={fact.label} className="min-w-0 px-4 py-3 first:pl-0 md:border-l md:border-[var(--border-subtle)] md:first:border-l-0 md:first:pl-0">
            <dt className="text-[10.5px] font-semibold uppercase tracking-[0.08em] text-[var(--text-quaternary)]">{fact.label}</dt>
            <dd className="mt-1.5 text-[12.5px] font-medium leading-snug text-[var(--text-secondary)] [overflow-wrap:anywhere]">
              {fact.value}
            </dd>
          </div>
        ))}
      </dl>
    </div>
  );
}

/**
 * 标题上方的 eyebrow：一行 mono 路由身份标记
 *   ▎GET · /api/users/{id}
 *
 * 比 endpoint 卡更轻，承担「这一页是哪个路由」的快速识别。
 * Method 着色继承全站方法色谱，扫一眼就能区分类型。
 */
function OperationEyebrow({ method, path }: { method: string; path: string }) {
  const m = method.toLowerCase();
  return (
    <div className="docs-route-kicker mb-5 inline-flex max-w-full items-center gap-2.5 font-mono text-[11.5px]">
      <span
        className="rounded-md border px-2 py-1 font-semibold uppercase tracking-[0.12em]"
        style={{ color: `var(--method-${m}-text, var(--text-secondary))` }}
      >
        {method}
      </span>
      <span className="text-[var(--text-quaternary)]">/</span>
      <span className="truncate text-[var(--text-tertiary)]">{path}</span>
    </div>
  );
}

// ── 占位面板 ──

function PlaceholderPanel({ icon: Icon, title, description }: {
  icon: typeof Bug;
  title: string;
  description: string;
}) {
  return (
    <div className="flex flex-col items-center justify-center px-8 py-36 text-center">
      <div className="mb-5 rounded-2xl bg-[var(--bg-subtle)] p-5">
        <Icon size={32} className="text-[var(--text-quaternary)]" />
      </div>
      <h2 className="text-[17px] font-semibold text-[var(--text-primary)]">{title}</h2>
      <p className="mt-2.5 max-w-[36ch] text-[14px] leading-relaxed text-[var(--text-tertiary)]">
        {description}
      </p>
    </div>
  );
}

export function OperationDetail({
  op,
  children,
  debugPanel,
}: {
  op: CanonicalOperation;
  /** 由服务端预渲染的文档 Tab 内容（OperationWiki） */
  children: ReactNode;
  /** 调试 Tab 插槽内容 */
  debugPanel?: ReactNode;
}) {
  const [active, setActive] = useState<TabKey>("doc");

  return (
    <div>
      {/* 顶部锚点：供 OnThisPage 的「概览」项跳转 */}
      <span id="operation-top" aria-hidden />

      {/* Frontmatter：克制的 API reference 头部，让 endpoint object 承担主视觉。 */}
      <div className="docs-reference-hero">
        <div className="mx-auto max-w-[1100px] px-6 pb-8 pt-12 sm:px-10 sm:pt-16">
          <OperationEyebrow method={op.method} path={op.path} />

          <div className="max-w-[820px]">
            <h1 className="text-[clamp(1.75rem,3vw,2.35rem)] font-semibold leading-[1.08] tracking-tight text-[var(--text-primary)]">
              {op.summary || op.operationId}
            </h1>

            {op.operationId && (
              <code className="docs-mono-chip mt-4 max-w-full truncate">
                {op.operationId}
              </code>
            )}

            {op.description && (
              <HtmlText
                as="div"
                text={op.description}
                className="mt-5 max-w-[70ch] text-[15px] leading-[1.78] text-[var(--text-secondary)] [&>p]:mt-2.5 [&>p:first-child]:mt-0 [&_ul]:mt-1.5 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mt-1.5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mt-0.5 [&_code]:rounded [&_code]:bg-[var(--bg-subtle)] [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[13px]"
              />
            )}
          </div>

          <OperationFactsStrip op={op} />

          <EndpointBar
            method={op.method}
            path={op.path}
            meta={[
              op.requestBody?.contentType
                ? { label: "请求", value: op.requestBody.contentType }
                : null,
              op.responses?.[0]?.contentType
                ? { label: "响应", value: op.responses[0].contentType }
                : null,
            ].filter((m): m is { label: string; value: string } => Boolean(m))}
          />

          <div className="mt-7 border-t border-[var(--border-subtle)] pt-5">
            <div className="inline-flex items-center gap-1 rounded-xl border border-[var(--border-subtle)] bg-[var(--docs-surface-tint)] p-1">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = active === tab.key;
                return (
                  <button
                    key={tab.key}
                    onClick={() => setActive(tab.key)}
                    className={`relative flex cursor-pointer items-center gap-1.5 rounded-lg px-3.5 py-2 text-[13px] font-medium transition-all duration-200 active:translate-y-px ${
                      isActive
                        ? "bg-[var(--bg-surface)] text-[var(--text-primary)] shadow-[0_10px_22px_-20px_rgba(24,24,27,0.65)]"
                        : "text-[var(--text-tertiary)] hover:text-[var(--text-secondary)]"
                    }`}
                  >
                    <Icon
                      size={15}
                      weight={isActive ? "fill" : "regular"}
                      className={`transition-colors duration-200 ${isActive ? "text-[var(--accent)]" : "text-[var(--text-quaternary)]"}`}
                    />
                    {tab.label}
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-[1100px] px-6 pb-16 pt-11 sm:px-10 sm:pt-12">
        {active === "doc" && children}
        {active === "debug" && (
          debugPanel ?? (
            <PlaceholderPanel
              icon={Bug}
              title="调试测试"
              description="在这里直接调试和测试该接口，发送请求并查看响应结果。即将上线。"
            />
          )
        )}
      </div>
    </div>
  );
}
