import type { JsonSchema } from "../lib/api";

/**
 * 根据 JSON 字符串与 JsonSchema 生成行级注释映射。
 * key = 行号（0-based），value = 该行字段的 description。
 *
 * 算法依据 generateExample 输出的 2-space pretty-print 格式，
 * 通过缩进深度定位当前 schema 层级。
 */
export function buildLineAnnotations(
  jsonStr: string,
  schema: JsonSchema,
): Map<number, string> {
  const lines = jsonStr.split("\n");
  const result = new Map<number, string>();

  // 归一化：数组根 → 取 items
  let root = schema;
  if (root.type === "array" && root.items?.properties) root = root.items;
  if (!root.properties) return result;

  // schemaAtDepth[d] = 深度 d 处正在列举属性的 schema
  const schemaAtDepth: (JsonSchema | undefined)[] = [root];

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const leadingSpaces = line.match(/^(\s*)/)?.[1].length ?? 0;
    // 2-space indent，根对象字段起始缩进 2 → depth 0
    const depth = Math.floor(leadingSpaces / 2) - 1;

    const keyMatch = line.match(/^\s*"([^"]+)"\s*:\s*(.*)/);
    if (!keyMatch || depth < 0) continue;

    const key = keyMatch[1];
    const rest = keyMatch[2].trim().replace(/,\s*$/, "");

    const currentSchema = schemaAtDepth[depth];
    const fieldSchema = currentSchema?.properties?.[key];

    if (fieldSchema?.description) {
      result.set(i, fieldSchema.description);
    }

    // 注册子级 schema
    if (rest === "{" && fieldSchema) {
      schemaAtDepth[depth + 1] = fieldSchema;
    } else if (rest === "[" && fieldSchema) {
      // 数组比 object 多一层缩进（[ 后还有 {）
      const items = fieldSchema.items ?? fieldSchema;
      schemaAtDepth[depth + 2] = items;
    }
  }

  return result;
}
