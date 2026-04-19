"use client";

import { type ReactNode, useState } from "react";

type RequestBodyTabKey = "schema" | "example";

export function RequestBodyTabs({
  schemaPanel,
  examplePanel,
}: {
  schemaPanel?: ReactNode;
  examplePanel?: ReactNode;
}) {
  const tabs = [
    schemaPanel ? { key: "schema" as const, label: "字段结构" } : null,
    examplePanel ? { key: "example" as const, label: "JSON 预览" } : null,
  ].filter(Boolean) as Array<{ key: RequestBodyTabKey; label: string }>;

  const [activeKey, setActiveKey] = useState<RequestBodyTabKey>(tabs[0]?.key ?? "schema");

  if (tabs.length === 0) return null;

  const activePanel = activeKey === "schema" ? schemaPanel : examplePanel;

  return (
    <div className="overflow-hidden rounded-2xl border border-[var(--border-default)] bg-[var(--bg-surface)]">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--border-subtle)] px-5 py-3">
        <div className="flex gap-1.5">
          {tabs.map((tab) => {
            const isActive = activeKey === tab.key;

            return (
              <button
                key={tab.key}
                onClick={() => setActiveKey(tab.key)}
                className={`cursor-pointer rounded-full px-3 py-1.5 text-[12px] font-medium transition-all duration-200 ${
                  isActive
                    ? "bg-[var(--text-primary)] text-[var(--bg-surface)] shadow-[0_8px_20px_-14px_rgba(15,23,42,0.45)]"
                    : "text-v-gray-400 hover:bg-v-gray-50 hover:text-v-gray-600"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      <div>{activePanel}</div>
    </div>
  );
}
