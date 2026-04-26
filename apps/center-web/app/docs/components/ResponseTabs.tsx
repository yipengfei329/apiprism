"use client";

import { type ReactNode, useState } from "react";
import type { CanonicalResponse } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { StatusBadge } from "./StatusBadge";
import { SchemaTable } from "./SchemaTable";
import { RequestBodyTabs } from "./RequestBodyTabs";
import { EmptyPanel } from "./EmptyPanel";
import { schemaTypeLabel } from "./schemaUtils";

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
      <div className="mb-4 flex flex-wrap items-center gap-1.5">
        {responses.map((response, index) => {
          const isActive = index === activeIndex;

          return (
            <button
              key={response.statusCode}
              type="button"
              onClick={() => setActiveIndex(index)}
              aria-pressed={isActive}
              className={`cursor-pointer rounded-md transition-opacity duration-200 ${
                isActive ? "opacity-100" : "opacity-40 hover:opacity-75"
              }`}
            >
              <StatusBadge code={response.statusCode} />
            </button>
          );
        })}
      </div>

      <div className="mb-4">
        <div className="flex flex-wrap items-center gap-2.5">
          {active.contentType && (
            <code className="rounded-full bg-[var(--bg-subtle)] px-2.5 py-1 font-mono text-[11px] text-v-gray-500">
              {active.contentType}
            </code>
          )}
          {active.schema && (
            <span className="rounded-full bg-[var(--bg-subtle)] px-2.5 py-1 font-mono text-[11px] text-v-gray-500">
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
            <div className="px-5 py-1">
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
    </div>
  );
}
