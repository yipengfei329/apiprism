"use client";

import { useState } from "react";
import {
  BookOpen,
  Bug,
} from "@phosphor-icons/react";
import { type ReactNode } from "react";
import { CanonicalOperation } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { MethodBadge } from "./MethodBadge";
import { AgentDocLink } from "./AgentDocLink";

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

function HeaderMetaPill({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="inline-flex items-center gap-2 rounded-full border border-v-gray-100 bg-v-gray-50 px-3 py-1.5 text-[11px] text-v-gray-500">
      <span className="uppercase tracking-[0.08em] text-v-gray-400">{label}</span>
      <span className="font-mono text-v-gray-600">{value}</span>
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
}: {
  op: CanonicalOperation;
  service: string;
  environment: string;
  group: string;
  /** 由服务端预渲染的文档 Tab 内容（OperationWiki） */
  children: ReactNode;
}) {
  const [active, setActive] = useState<TabKey>("doc");

  return (
    <div>
      {/* ── 头部区域 ── */}
      <div className="border-b border-v-gray-100">
        <div className="mx-auto max-w-[1100px] px-4 pb-8 pt-8 sm:px-8 sm:pt-14">
          {/* 标题 */}
          <div className="flex items-start justify-between gap-4">
            <h1
              className="text-[clamp(1.6rem,3vw,2.2rem)] font-semibold leading-[1.15] text-v-black v-slide-up"
              style={{ letterSpacing: "-0.025em" }}
            >
              {op.summary || op.operationId}
            </h1>
            <AgentDocLink path={`/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group)}/${encodeURIComponent(op.operationId)}/apidocs.md`} />
          </div>

          {op.operationId && (
            <div className="mt-3">
              <HeaderMetaPill label="Operation" value={op.operationId} />
            </div>
          )}

          {/* 描述 */}
          {op.description && (
            <HtmlText
              as="div"
              text={op.description}
              className="mt-4 max-w-[68ch] text-[15px] leading-[1.8] text-v-gray-600 v-slide-up v-delay-1 [&>p]:mt-2 [&>p:first-child]:mt-0 [&_ul]:mt-1.5 [&_ul]:list-disc [&_ul]:pl-5 [&_ol]:mt-1.5 [&_ol]:list-decimal [&_ol]:pl-5 [&_li]:mt-0.5 [&_code]:rounded [&_code]:bg-v-gray-50 [&_code]:px-1 [&_code]:py-0.5 [&_code]:text-[13px]"
            />
          )}

          <div className="mt-6 overflow-hidden rounded-xl border border-v-gray-100 bg-[var(--bg-surface)] v-fade-in">
            <div className="flex flex-wrap items-center gap-2 border-b border-v-gray-100 px-5 py-3">
              <span className="text-[10px] font-semibold uppercase tracking-[0.14em] text-v-gray-400">
                Endpoint
              </span>
              {op.requestBody?.contentType && (
                <HeaderMetaPill label="Request" value={op.requestBody.contentType} />
              )}
              {op.responses?.[0]?.contentType && (
                <HeaderMetaPill label="Response" value={op.responses[0].contentType} />
              )}
            </div>
            <div className="flex min-w-0 flex-col gap-3 px-5 py-5 md:flex-row md:items-center">
              <MethodBadge method={op.method} size="lg" />
              <code className="block overflow-x-auto font-mono text-[15px] font-medium leading-[1.7] text-v-black md:text-[16px]">
                {op.path}
              </code>
            </div>
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-[1100px] px-4 sm:px-8">
        {/* ── Tab 栏 ── */}
        <div className="-mt-4 flex">
          <div className="inline-flex rounded-full border border-v-gray-100 bg-[var(--bg-surface)] p-1">
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
        </div>
      </div>

      {/* ── 内容区域 ── */}
      <div className="mx-auto max-w-[1100px] px-4 py-6 sm:px-8 sm:py-8">
        {active === "doc" && children}
        {active === "debug" && (
          <PlaceholderPanel
            icon={Bug}
            title="调试测试"
            description="在这里直接调试和测试该接口，发送请求并查看响应结果。即将上线。"
          />
        )}
      </div>
    </div>
  );
}
