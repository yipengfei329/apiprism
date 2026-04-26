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
    <div>
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex border-b border-[var(--border-subtle)]">
          {tabs.map((tab) => {
            const isActive = activeKey === tab.key;

            return (
              <button
                key={tab.key}
                onClick={() => setActiveKey(tab.key)}
                className={`relative -mb-px cursor-pointer px-3.5 py-2 text-[12px] font-medium transition-colors duration-200 ${
                  isActive
                    ? "border-b border-[var(--text-primary)] text-[var(--text-primary)]"
                    : "border-b border-transparent text-v-gray-400 hover:text-v-gray-600"
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="docs-table-shell overflow-hidden rounded-xl">{activePanel}</div>
    </div>
  );
}
