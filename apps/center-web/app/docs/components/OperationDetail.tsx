"use client";

import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import {
  ArrowUp,
  BookOpen,
  Bug,
} from "@phosphor-icons/react";
import { CanonicalOperation } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { AgentDocLink } from "./AgentDocLink";
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
 * 接口快速摘要：在标题/描述与 endpoint 卡之间，一行小灰色 dotted 列表，
 * 让程序员扫一眼就能 grasp:「需认证 · application/json · 200 + 4 错误码」
 */
function QuickFacts({ op }: { op: CanonicalOperation }) {
  const facts: ReactNode[] = [];

  if (op.securityRequirements && op.securityRequirements.length > 0) {
    facts.push(
      <span key="auth" className="inline-flex items-center gap-1.5">
        <span className="docs-status-dot" style={{ color: "var(--accent)" }} aria-hidden />
        需认证
      </span>,
    );
  }

  if (op.requestBody?.contentType) {
    facts.push(
      <span key="req">
        请求 <span className="font-mono text-[var(--text-secondary)]">{op.requestBody.contentType}</span>
      </span>,
    );
  }

  if (op.responses && op.responses.length > 0) {
    const okCount = op.responses.filter((r) => {
      const code = parseInt(r.statusCode, 10);
      return code >= 200 && code < 300;
    }).length;
    const errorCount = op.responses.length - okCount;
    facts.push(
      <span key="resp" className="tabular-nums">
        {op.responses.length} 个响应
        {errorCount > 0 && (
          <span className="text-[var(--text-quaternary)]">
            {" "}（{okCount} 成功 + {errorCount} 错误）
          </span>
        )}
      </span>,
    );
  }

  if (op.parameters && op.parameters.length > 0) {
    facts.push(
      <span key="params" className="tabular-nums">
        {op.parameters.length} 个参数
      </span>,
    );
  }

  if (facts.length === 0) return null;

  return (
    <div className="mt-5 flex flex-wrap items-center gap-x-3 gap-y-1.5 text-[12.5px] text-[var(--text-tertiary)]">
      {facts.map((fact, i) => (
        <span key={i} className="inline-flex items-center">
          {fact}
          {i < facts.length - 1 && (
            <span className="ml-3 text-[var(--text-quaternary)]">·</span>
          )}
        </span>
      ))}
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
      <div className="mb-5 rounded-2xl bg-v-gray-50 p-5">
        <Icon size={32} className="text-v-gray-400" />
      </div>
      <h2 className="text-[17px] font-semibold text-v-black">{title}</h2>
      <p className="mt-2.5 max-w-[36ch] text-[14px] leading-relaxed text-v-gray-400">
        {description}
      </p>
    </div>
  );
}

export function OperationDetail({
  op,
  service,
  environment,
  group,
  children,
  debugPanel,
}: {
  op: CanonicalOperation;
  service: string;
  environment: string;
  group: string;
  /** 由服务端预渲染的文档 Tab 内容（OperationWiki） */
  children: ReactNode;
  /** 调试 Tab 插槽内容 */
  debugPanel?: ReactNode;
}) {
  const [active, setActive] = useState<TabKey>("doc");
  const [stuck, setStuck] = useState(false);
  const sentinelRef = useRef<HTMLDivElement>(null);

  // 通过哨兵元素是否离开视口来判断 Tab 栏是否吸顶
  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel) return;
    const obs = new IntersectionObserver(
      ([entry]) => setStuck(!entry.isIntersecting),
      { threshold: 0 },
    );
    obs.observe(sentinel);
    return () => obs.disconnect();
  }, []);

  const scrollToTop = useCallback(() => {
    const el = document.getElementById("operation-top");
    if (!el) return;
    el.scrollIntoView({ behavior: "smooth", block: "start" });
  }, []);

  return (
    <div>
      {/* 顶部锚点：供"返回顶部"按钮和页内导航的"概览"项跳转 */}
      <span id="operation-top" aria-hidden />

      {/* ── 头部区域 ── */}
      <div className="border-b border-[var(--border-default)]">
        <div className="mx-auto max-w-[1100px] px-6 pb-10 pt-16 sm:px-10 sm:pt-20">
          {/* 标题：纯字重，无渐变、无装饰 */}
          <div className="flex items-start justify-between gap-4">
            <h1
              className="text-[clamp(1.6rem,2.8vw,2.2rem)] font-semibold leading-[1.15] text-[var(--text-primary)]"
              style={{ letterSpacing: "-0.03em" }}
            >
              {op.summary || op.operationId}
            </h1>
            <AgentDocLink path={`/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}/${encodeURIComponent(op.operationId)}/apidocs.md`} />
          </div>

          {op.operationId && (
            <p className="mt-3 font-mono text-[12.5px] text-[var(--text-tertiary)]">
              {op.operationId}
            </p>
          )}

          {/* 描述 */}
          {op.description && (
            <HtmlText
              as="div"
              text={op.description}
              className="mt-5 max-w-[68ch] text-[15px] leading-[1.75] text-[var(--text-secondary)] [&>p]:mt-2.5 [&>p:first-child]:mt-0 [&_ul]:mt-1.5 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mt-1.5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mt-0.5 [&_code]:rounded [&_code]:bg-[var(--bg-subtle)] [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[13px]"
            />
          )}

          {/* Quick facts：一行小灰色摘要——程序员一眼扫到关键事实 */}
          <QuickFacts op={op} />

          {/* Endpoint 物件：method + path + 复制按钮 */}
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
        </div>
      </div>

      {/* 哨兵元素：用于检测 Tab 栏是否进入吸顶状态 */}
      <div ref={sentinelRef} aria-hidden className="h-0" />

      {/* ── Tab 栏（吸顶） ── */}
      <div className="sticky top-0 z-20">
        {/* 吸顶时淡入的背景层（半透明 + 模糊） */}
        <div
          aria-hidden
          className="pointer-events-none absolute inset-0 -z-10 transition-opacity duration-200"
          style={{
            backgroundColor: "var(--bg-canvas)",
            backdropFilter: "blur(10px)",
            WebkitBackdropFilter: "blur(10px)",
            borderBottom: "1px solid var(--border-default)",
            opacity: stuck ? 0.92 : 0,
          }}
        />
        <div className="mx-auto max-w-[1100px] px-4 sm:px-8">
          <div
            className="flex items-center justify-between gap-3"
            style={{
              marginTop: stuck ? 0 : -16,
              paddingTop: stuck ? 10 : 0,
              paddingBottom: stuck ? 10 : 0,
              transition: "margin-top 200ms ease, padding 200ms ease",
            }}
          >
            <div className="inline-flex rounded-full bg-[var(--bg-subtle)] p-1">
              {tabs.map((tab) => {
                const Icon = tab.icon;
                const isActive = active === tab.key;
                return (
                  <button
                    key={tab.key}
                    onClick={() => setActive(tab.key)}
                    className={`relative flex cursor-pointer items-center gap-1.5 rounded-full px-4 py-2 text-[13px] font-medium transition-all duration-300 ${
                      isActive
                        ? "bg-[var(--text-primary)] text-[var(--bg-surface)]"
                        : "text-[var(--text-tertiary)] hover:text-[var(--text-secondary)] hover:bg-[var(--bg-subtle)]"
                    }`}
                  >
                    <Icon
                      size={15}
                      weight={isActive ? "fill" : "regular"}
                      className={`transition-colors duration-200 ${isActive ? "text-[var(--bg-surface)]" : "text-[var(--text-quaternary)]"}`}
                    />
                    {tab.label}
                  </button>
                );
              })}
            </div>
            <button
              onClick={scrollToTop}
              aria-label="返回顶部"
              title="返回顶部"
              className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full text-[var(--text-tertiary)] transition-all duration-200 hover:bg-[var(--bg-subtle)] hover:text-[var(--text-primary)]"
              style={{
                opacity: stuck ? 1 : 0,
                pointerEvents: stuck ? "auto" : "none",
              }}
            >
              <ArrowUp size={14} weight="bold" />
            </button>
          </div>
        </div>
      </div>

      {/* ── 内容区域 ── */}
      <div className="mx-auto max-w-[1100px] px-4 py-6 sm:px-8 sm:py-8">
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
