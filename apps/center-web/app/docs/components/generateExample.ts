import type { JsonSchema } from "../lib/api";

/**
 * 从 JsonSchema 递归生成示例数据。
 * 优先使用 schema.example，其次按 type/format 推导默认值。
 */
export function generateExample(
  schema: JsonSchema | null | undefined,
  visited: WeakSet<object> = new WeakSet(),
): unknown {
  if (!schema) return null;

  // 循环引用保护
  if (schema.$circular || visited.has(schema)) return "[circular]";
  visited.add(schema);

  // 优先使用显式 example
  if (schema.example !== undefined && schema.example !== null) return schema.example;

  // 枚举取第一个值
  if (schema.enum && schema.enum.length > 0) return schema.enum[0];

  const { type, format, properties, items, additionalProperties } = schema;

  // 有 properties 视为 object
  if (properties && Object.keys(properties).length > 0) {
    const obj: Record<string, unknown> = {};
    for (const [key, prop] of Object.entries(properties)) {
      obj[key] = generateExample(prop, visited);
    }
    return obj;
  }

  // Map<K,V> 类型：用示例 key 展示值类型
  if (additionalProperties) {
    return { key: generateExample(additionalProperties, visited) };
  }

  switch (type) {
    case "string":
      return stringByFormat(format);
    case "integer":
    case "number":
      return 0;
    case "boolean":
      return false;
    case "array":
      if (items) return [generateExample(items, visited)];
      return [];
    case "object":
      return {};
    default:
      return null;
  }
}

// 根据 format 生成合理的字符串示例
function stringByFormat(format?: string): string {
  switch (format) {
    case "date-time":
      return "2025-01-01T00:00:00Z";
    case "date":
      return "2025-01-01";
    case "time":
      return "12:00:00";
    case "email":
      return "user@example.com";
    case "uri":
    case "url":
      return "https://example.com";
    case "uuid":
      return "550e8400-e29b-41d4-a716-446655440000";
    case "ipv4":
      return "192.168.1.1";
    case "ipv6":
      return "::1";
    default:
      return "string";
  }
}
