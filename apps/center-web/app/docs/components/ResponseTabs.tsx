"use client";

import { type ReactNode, useState } from "react";
import type { CanonicalResponse } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { StatusBadge } from "./StatusBadge";
import { SchemaTable } from "./SchemaTable";

export function ResponseTabs({
  responses,
  examples,
}: {
  responses: CanonicalResponse[];
  /** 由服务端预渲染的各状态码示例，与 responses 下标对齐 */
  examples?: ReactNode[];
}) {
  const [activeIndex, setActiveIndex] = useState(0);

  if (responses.length === 0) return null;

  const active = responses[activeIndex];

  return (
    <div>
      {/* Tab 栏 */}
      <div className="mb-4 flex gap-1.5">
        {responses.map((r, i) => (
          <button
            key={r.statusCode}
            onClick={() => setActiveIndex(i)}
            className={`cursor-pointer rounded-lg px-3 py-1.5 font-mono text-[13px] font-medium transition-all duration-300 ${
              i === activeIndex
                ? "bg-white text-v-black v-glass-subtle"
                : "text-v-gray-400 hover:text-v-gray-600 hover:bg-v-gray-50/50"
            }`}
            style={i === activeIndex ? { borderRadius: "0.5rem" } : undefined}
          >
            <StatusBadge code={r.statusCode} />
          </button>
        ))}
      </div>

      {/* Tab 面板 */}
      <div>
        {/* 描述与 content type */}
        <div className="mb-3 flex flex-wrap items-center gap-3">
          {active.description && (
            <HtmlText text={active.description} className="text-[13px] text-v-gray-600" />
          )}
          {active.contentType && (
            <code className="ml-auto font-mono text-[11px] text-v-gray-400">
              {active.contentType}
            </code>
          )}
        </div>

        {/* Schema + JSON 示例统一容器 */}
        {(active.schema || examples?.[activeIndex] != null) && (
          <div className="overflow-hidden rounded-2xl border border-[#E8E8EC]">
            {active.schema && (
              <div className="bg-v-gray-50/40 px-5 py-1">
                <SchemaTable schema={active.schema} />
              </div>
            )}
            {examples?.[activeIndex] != null && (
              <>{examples[activeIndex]}</>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
