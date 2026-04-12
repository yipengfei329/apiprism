"use client";

import { useState } from "react";
import type { JsonSchema } from "../lib/api";
import { schemaTypeLabel } from "./schemaUtils";

export { schemaTypeLabel };

const MAX_DEPTH = 6;

// ── 展开/折叠 chevron ──

function Chevron({ expanded }: { expanded: boolean }) {
  return (
    <svg
      className={`h-3.5 w-3.5 shrink-0 text-[#B0B0B8] transition-transform duration-150 ${expanded ? "rotate-90" : ""}`}
      viewBox="0 0 16 16"
      fill="currentColor"
    >
      <path d="M6.22 4.22a.75.75 0 011.06 0l3.25 3.25a.75.75 0 010 1.06l-3.25 3.25a.75.75 0 01-1.06-1.06L8.94 8 6.22 5.28a.75.75 0 010-1.06z" />
    </svg>
  );
}

// ── 单条属性 ──

function PropertyRow({
  name,
  schema,
  isRequired,
  depth,
  isLast,
}: {
  name: string;
  schema: JsonSchema;
  isRequired: boolean;
  depth: number;
  isLast: boolean;
}) {
  const hasChildren =
    !schema.$circular &&
    depth < MAX_DEPTH &&
    ((schema.properties && Object.keys(schema.properties).length > 0) ||
      (schema.type === "array" &&
        schema.items?.properties &&
        Object.keys(schema.items.properties).length > 0));

  const [expanded, setExpanded] = useState(depth === 0);

  const childSchema =
    schema.type === "array" && schema.items?.properties
      ? schema.items
      : schema.properties
        ? schema
        : null;

  return (
    <div className={!isLast ? "border-b border-[#F0F0F3]" : ""}>
      {/* 属性主体 */}
      <div className="flex items-start gap-2 px-4 py-3.5">
        {/* 展开按钮 */}
        <div className="flex shrink-0 items-center pt-[3px]" style={{ width: 18 }}>
          {hasChildren && (
            <button
              onClick={() => setExpanded(!expanded)}
              className="flex cursor-pointer items-center justify-center transition-colors hover:text-[#636366]"
            >
              <Chevron expanded={expanded} />
            </button>
          )}
        </div>

        {/* 属性信息 */}
        <div className="min-w-0 flex-1">
          {/* 第一行：name + type + required */}
          <div className="flex flex-wrap items-center gap-2">
            <span
              className={`font-mono text-[13px] font-semibold leading-snug ${
                schema.deprecated
                  ? "text-[#8E8E93] line-through"
                  : "text-[#1C1C1E]"
              }`}
            >
              {name}
            </span>
            <code
              className={`rounded-[5px] px-1.5 py-[1px] font-mono text-[11px] leading-snug ${
                schema.deprecated
                  ? "bg-[#F2F2F7] text-[#8E8E93]"
                  : "bg-[#EEF4FF] text-[#0063CC]"
              }`}
            >
              {schemaTypeLabel(schema)}
            </code>
            {isRequired && (
              <span className="font-mono text-[10px] font-semibold leading-snug text-[#E5484D]">
                必填
              </span>
            )}
          </div>

          {/* 第二行：description */}
          {schema.description && (
            <p
              className={`mt-1.5 text-[13px] leading-[1.65] ${
                schema.deprecated ? "text-[#8E8E93]" : "text-[#636366]"
              }`}
            >
              {schema.description}
            </p>
          )}

          {/* 枚举值 */}
          {schema.enum && schema.enum.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-1">
              {schema.enum.map((v, i) => (
                <span
                  key={i}
                  className="rounded-[5px] bg-[#F2F2F7] px-1.5 py-[1px] font-mono text-[10px] text-[#636366]"
                >
                  {String(v)}
                </span>
              ))}
            </div>
          )}

          {/* 示例值 */}
          {schema.example !== undefined && schema.example !== null && (
            <div className="mt-1.5 flex items-center gap-1.5">
              <span className="text-[11px] text-[#8E8E93]">示例:</span>
              <code className="rounded-[5px] bg-[#F2F2F7] px-1.5 py-[1px] font-mono text-[11px] text-[#636366]">
                {String(schema.example)}
              </code>
            </div>
          )}

          {/* 循环引用标记 */}
          {schema.$circular && (
            <span className="mt-1.5 inline-block rounded-[5px] bg-[#FFF7ED] px-1.5 py-[1px] text-[10px] font-medium text-[#C2410C]">
              循环引用
            </span>
          )}
        </div>
      </div>

      {/* 子属性：左边框缩进 */}
      {expanded && hasChildren && childSchema?.properties && (
        <div className="ml-[26px] border-l-[1.5px] border-[#E8E8EC]">
          <PropertyList
            properties={childSchema.properties}
            requiredFields={childSchema.required ?? []}
            depth={depth + 1}
          />
        </div>
      )}
    </div>
  );
}

// ── 属性列表 ──

function PropertyList({
  properties,
  requiredFields,
  depth,
}: {
  properties: Record<string, JsonSchema>;
  requiredFields: string[];
  depth: number;
}) {
  const entries = Object.entries(properties);
  return (
    <>
      {entries.map(([name, propSchema], i) => (
        <PropertyRow
          key={name}
          name={name}
          schema={propSchema}
          isRequired={requiredFields.includes(name)}
          depth={depth}
          isLast={i === entries.length - 1}
        />
      ))}
    </>
  );
}

// ── 主组件 ──

export function SchemaTable({ schema }: { schema: JsonSchema }) {
  const target =
    schema.type === "array" && schema.items?.properties ? schema.items : schema;
  const properties = target.properties;
  if (!properties || Object.keys(properties).length === 0) return null;

  return (
    <div className="mt-3 overflow-hidden rounded-xl">
      <PropertyList
        properties={properties}
        requiredFields={target.required ?? []}
        depth={0}
      />
    </div>
  );
}
