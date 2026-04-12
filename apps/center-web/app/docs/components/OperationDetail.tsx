"use client";

import { useState } from "react";
import {
  BookOpen,
  Bug,
  Robot,
} from "@phosphor-icons/react";
import { type ReactNode } from "react";
import { CanonicalOperation } from "../lib/api";
import { AgentPanel } from "./AgentPanel";
import { MethodBadge } from "./MethodBadge";

type TabKey = "doc" | "debug" | "agent";

interface TabDef {
  key: TabKey;
  label: string;
  icon: typeof BookOpen;
}

const tabs: TabDef[] = [
  { key: "doc", label: "文档", icon: BookOpen },
  { key: "debug", label: "调试测试", icon: Bug },
  { key: "agent", label: "For Agent", icon: Robot },
];

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
  children,
}: {
  op: CanonicalOperation;
  service: string;
  environment: string;
  /** 由服务端预渲染的文档 Tab 内容（OperationWiki） */
  children: ReactNode;
}) {
  const [active, setActive] = useState<TabKey>("doc");

  return (
    <div>
      {/* ── 头部区域 ── */}
      <div className="bg-v-gray-50/60">
        <div className="mx-auto max-w-[1100px] px-8 pb-0 pt-14">
          {/* Method + Path */}
          <div className="mb-5 flex flex-wrap items-center gap-3">
            <MethodBadge method={op.method} size="lg" />
            <code className="font-mono text-[14px] font-medium text-v-gray-500">
              {op.path}
            </code>
          </div>

          {/* 标题 */}
          <h1
            className="text-[clamp(1.6rem,3vw,2.2rem)] font-semibold leading-[1.15] text-v-black"
            style={{ letterSpacing: "-0.025em" }}
          >
            {op.summary || op.operationId}
          </h1>

          {/* operationId */}
          {op.operationId && (
            <p className="mt-2.5 font-mono text-[12px] text-v-gray-400">{op.operationId}</p>
          )}

          {/* 描述 */}
          {op.description && (
            <p className="mt-4 max-w-[68ch] text-[15px] leading-[1.8] text-v-gray-600">
              {op.description}
            </p>
          )}

          {/* ── Tab 栏 ── */}
          <div className="mt-8 flex items-end gap-1">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = active === tab.key;
              return (
                <button
                  key={tab.key}
                  onClick={() => setActive(tab.key)}
                  className={`relative flex items-center gap-1.5 rounded-t-lg px-4 py-2.5 text-[13px] font-medium transition-all duration-200 ${
                    isActive
                      ? "bg-white text-v-link"
                      : "text-v-gray-400 hover:text-v-gray-600"
                  }`}
                >
                  <Icon
                    size={15}
                    weight={isActive ? "fill" : "regular"}
                    className={isActive ? "text-v-link" : "text-v-gray-400/60"}
                  />
                  {tab.label}
                </button>
              );
            })}
          </div>
        </div>
      </div>

      {/* ── 内容区域 ── */}
      <div className="mx-auto max-w-[1100px] px-8 py-12">
        {active === "doc" && children}
        {active === "debug" && (
          <PlaceholderPanel
            icon={Bug}
            title="调试测试"
            description="在这里直接调试和测试该接口，发送请求并查看响应结果。即将上线。"
          />
        )}
        {active === "agent" && (
          <AgentPanel
            service={service}
            environment={environment}
            operationId={op.operationId}
          />
        )}
      </div>
    </div>
  );
}
