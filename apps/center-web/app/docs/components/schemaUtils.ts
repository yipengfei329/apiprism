import type { JsonSchema } from "../lib/api";

/** 从 JSON Schema 中提取类型摘要字符串，如 "string", "integer (int64)", "array<Order>", "map<string>" */
export function schemaTypeLabel(schema: JsonSchema | null | undefined): string {
  if (!schema) return "—";
  const { type, format, items, additionalProperties } = schema;
  if (type === "array" && items) {
    return `array<${schemaTypeLabel(items)}>`;
  }
  // 仅当 additionalProperties 有实际类型信息时才视为 map；空对象 {} 不算 map
  if (additionalProperties && (additionalProperties.type || additionalProperties.properties || additionalProperties.items || additionalProperties.additionalProperties)) {
    return `map<${schemaTypeLabel(additionalProperties)}>`;
  }
  if (type && format) return `${type} (${format})`;
  if (type) return type;
  return "object";
}
