package ai.apiprism.center.markdown;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 JSON Schema 递归生成示例数据。
 * 优先使用 schema 中的 example 字段，其次按 type/format 推导默认值。
 */
class ExampleGenerator {

    private static final int MAX_DEPTH = 8;

    @SuppressWarnings("unchecked")
    static Object generate(Map<String, Object> schema) {
        return generate(schema, 0);
    }

    @SuppressWarnings("unchecked")
    private static Object generate(Map<String, Object> schema, int depth) {
        if (schema == null || depth > MAX_DEPTH) return null;

        // 优先使用显式 example
        Object example = schema.get("example");
        if (example != null) return example;

        // 枚举取第一个值
        Object enumValues = schema.get("enum");
        if (enumValues instanceof List<?> list && !list.isEmpty()) return list.get(0);

        String type = (String) schema.get("type");
        String format = (String) schema.get("format");

        // 有 properties 视为 object
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        if (properties != null && !properties.isEmpty()) {
            Map<String, Object> obj = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> propSchema) {
                    obj.put(entry.getKey(), generate((Map<String, Object>) propSchema, depth + 1));
                }
            }
            return obj;
        }

        if (type == null) return null;

        return switch (type) {
            case "string" -> stringByFormat(format);
            case "integer", "number" -> 0;
            case "boolean" -> false;
            case "array" -> {
                Object items = schema.get("items");
                if (items instanceof Map<?, ?> itemSchema) {
                    yield List.of(generate((Map<String, Object>) itemSchema, depth + 1));
                }
                yield List.of();
            }
            case "object" -> Map.of();
            default -> null;
        };
    }

    private static String stringByFormat(String format) {
        if (format == null) return "string";
        return switch (format) {
            case "date-time" -> "2025-01-01T00:00:00Z";
            case "date" -> "2025-01-01";
            case "time" -> "12:00:00";
            case "email" -> "user@example.com";
            case "uri", "url" -> "https://example.com";
            case "uuid" -> "550e8400-e29b-41d4-a716-446655440000";
            case "ipv4" -> "192.168.1.1";
            case "ipv6" -> "::1";
            default -> "string";
        };
    }
}
