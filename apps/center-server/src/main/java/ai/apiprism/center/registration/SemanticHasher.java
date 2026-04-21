package ai.apiprism.center.registration;

import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalServiceSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 基于规范化后的 API 语义内容计算 spec hash，而非原始文本字节。
 * 排序规则固定，确保相同接口定义无论原始格式/空白/字段顺序如何，始终产生相同哈希值。
 */
public class SemanticHasher {

    /**
     * 语义哈希算法版本前缀，算法逻辑变更时递增，强制所有服务在下次注册时创建新 revision。
     */
    public static final String VERSION = "smh:1";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SemanticHasher() {
    }

    public static String hash(CanonicalServiceSnapshot snapshot) {
        try {
            String canonical = buildCanonicalJson(snapshot);
            return sha256(VERSION + "\n" + canonical);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to compute semantic hash", e);
        }
    }

    /**
     * 序列化单个 operation 为确定性 JSON，供 diff 比较使用。
     */
    public static String operationJson(CanonicalOperation op) {
        try {
            return MAPPER.writeValueAsString(buildOperation(op));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize operation", e);
        }
    }

    static String buildCanonicalJson(CanonicalServiceSnapshot s) throws JsonProcessingException {
        Map<String, Object> root = new TreeMap<>();
        root.put("title", s.getTitle());
        root.put("version", s.getVersion());
        root.put("serverUrls", sorted(s.getServerUrls()));

        List<Map<String, Object>> groups = s.getGroups() == null ? List.of() :
                s.getGroups().stream()
                        .sorted(Comparator.comparing(
                                CanonicalGroup::getSlug, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(SemanticHasher::buildGroup)
                        .toList();
        root.put("groups", groups);

        return MAPPER.writeValueAsString(root);
    }

    private static Map<String, Object> buildGroup(CanonicalGroup g) {
        Map<String, Object> m = new TreeMap<>();
        m.put("description", g.getDescription());
        m.put("name", g.getName());
        m.put("slug", g.getSlug());
        m.put("operations", g.getOperations() == null ? List.of() :
                g.getOperations().stream()
                        .sorted(Comparator
                                .comparing((CanonicalOperation op) -> op.getMethod().toUpperCase())
                                .thenComparing(CanonicalOperation::getPath))
                        .map(SemanticHasher::buildOperation)
                        .toList());
        return m;
    }

    static Map<String, Object> buildOperation(CanonicalOperation op) {
        Map<String, Object> m = new TreeMap<>();
        m.put("description", op.getDescription());
        m.put("method", op.getMethod());
        m.put("operationId", op.getOperationId());
        m.put("parameters", op.getParameters() == null ? List.of() :
                op.getParameters().stream()
                        .sorted(Comparator.comparing(CanonicalParameter::getLocation)
                                .thenComparing(CanonicalParameter::getName))
                        .map(SemanticHasher::buildParameter)
                        .toList());
        m.put("path", op.getPath());
        m.put("requestBody", op.getRequestBody() == null ? null : buildRequestBody(op.getRequestBody()));
        m.put("responses", op.getResponses() == null ? List.of() :
                op.getResponses().stream()
                        .sorted(Comparator.comparing(CanonicalResponse::getStatusCode))
                        .map(SemanticHasher::buildResponse)
                        .toList());
        m.put("securityRequirements", sorted(op.getSecurityRequirements()));
        m.put("summary", op.getSummary());
        m.put("tags", sorted(op.getTags()));
        return m;
    }

    private static Map<String, Object> buildParameter(CanonicalParameter p) {
        Map<String, Object> m = new TreeMap<>();
        m.put("description", p.getDescription());
        m.put("location", p.getLocation());
        m.put("name", p.getName());
        m.put("required", p.isRequired());
        m.put("schema", sortedSchema(p.getSchema()));
        return m;
    }

    private static Map<String, Object> buildRequestBody(CanonicalRequestBody rb) {
        Map<String, Object> m = new TreeMap<>();
        m.put("contentType", rb.getContentType());
        m.put("required", rb.isRequired());
        m.put("schema", sortedSchema(rb.getSchema()));
        return m;
    }

    private static Map<String, Object> buildResponse(CanonicalResponse r) {
        Map<String, Object> m = new TreeMap<>();
        m.put("contentType", r.getContentType());
        m.put("description", r.getDescription());
        m.put("schema", sortedSchema(r.getSchema()));
        m.put("statusCode", r.getStatusCode());
        return m;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> sortedSchema(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        Map<String, Object> sorted = new TreeMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            if (v instanceof Map) {
                v = sortedSchema((Map<String, Object>) v);
            } else if (v instanceof List) {
                v = sortedSchemaList((List<Object>) v);
            }
            sorted.put(entry.getKey(), v);
        }
        return sorted;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> sortedSchemaList(List<Object> list) {
        if (list == null) {
            return null;
        }
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(sortedSchema((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(sortedSchemaList((List<Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    private static List<String> sorted(List<String> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().sorted().toList();
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
