"use client";

import { type ReactNode, useState } from "react";
import type { CanonicalResponse } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { StatusBadge } from "./StatusBadge";
import { SchemaTable } from "./SchemaTable";
import { RequestBodyTabs } from "./RequestBodyTabs";
import { EmptyPanel } from "./EmptyPanel";
import { schemaTypeLabel } from "./schemaUtils";

function ResponseMeta({
  label,
  value,
}: {
  label: string;
  value: ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-v-gray-100 bg-v-gray-50 px-4 py-3">
      <p className="text-[10px] font-semibold uppercase tracking-[0.12em] text-v-gray-400">
        {label}
      </p>
      <div className="mt-2 text-[13px] text-v-black">{value}</div>
    </div>
  );
}

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
  const activeExample = examples?.[activeIndex] ?? null;

  return (
    <div>
      <div className="mb-4 flex flex-wrap gap-1.5">
        {responses.map((response, index) => {
          const isActive = index === activeIndex;

          return (
            <button
              key={response.statusCode}
              onClick={() => setActiveIndex(index)}
              className={`cursor-pointer rounded-full px-3.5 py-2 font-mono text-[13px] font-medium transition-all duration-200 ${
                isActive
                  ? "bg-[var(--text-primary)] text-[var(--bg-surface)]"
                  : "bg-[var(--bg-surface)] text-v-gray-400 hover:bg-v-gray-50 hover:text-v-gray-600"
              }`}
            >
              <StatusBadge code={response.statusCode} inverted={isActive} />
            </button>
          );
        })}
      </div>

      <div className="mb-4 rounded-2xl border border-v-gray-100 bg-v-gray-50 px-5 py-4">
        <div className="flex flex-wrap items-center gap-2.5">
          <StatusBadge code={active.statusCode} />
          {active.contentType && (
            <code className="rounded-full bg-[var(--bg-surface)] px-2.5 py-1 font-mono text-[11px] text-v-gray-500">
              {active.contentType}
            </code>
          )}
          {active.schema && (
            <span className="rounded-full bg-v-gray-50 px-2.5 py-1 font-mono text-[11px] text-v-gray-500 v-ring-light">
              {schemaTypeLabel(active.schema)}
            </span>
          )}
        </div>
        {active.description && (
          <HtmlText
            text={active.description}
            className="mt-3 text-[13px] leading-[1.7] text-v-gray-600 [&>p]:mt-1 [&>p:first-child]:mt-0"
          />
        )}
      </div>

      <RequestBodyTabs
        schemaPanel={
          active.schema ? (
            <div className="bg-v-gray-50/30 px-5 py-1">
              <SchemaTable schema={active.schema} />
            </div>
          ) : (
            <EmptyPanel message="暂无结构定义" />
          )
        }
        examplePanel={
          activeExample ?? <EmptyPanel variant="example" message="暂无可生成的 JSON 示例" />
        }
      />

      <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
        <ResponseMeta label="Status" value={<code className="font-mono text-[12px] text-v-gray-500">{active.statusCode}</code>} />
        <ResponseMeta
          label="Content Type"
          value={
            <code className="font-mono text-[12px] text-v-gray-500">
              {active.contentType || "—"}
            </code>
          }
        />
      </div>
    </div>
  );
}
