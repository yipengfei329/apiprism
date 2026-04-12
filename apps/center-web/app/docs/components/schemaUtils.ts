import type { JsonSchema } from "../lib/api";

/** 从 JSON Schema 中提取类型摘要字符串，如 "string", "integer (int64)", "array<Order>" */
export function schemaTypeLabel(schema: JsonSchema | null | undefined): string {
  if (!schema) return "—";
  const { type, format, items } = schema;
  if (type === "array" && items) {
    return `array<${schemaTypeLabel(items)}>`;
  }
  if (type && format) return `${type} (${format})`;
  if (type) return type;
  return "object";
}
