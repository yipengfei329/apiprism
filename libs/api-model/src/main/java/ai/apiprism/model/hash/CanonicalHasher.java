package ai.apiprism.model.hash;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 对归一化后的接口定义计算稳定 hash，用于中心侧的变化检测。
 *
 * <p>仅覆盖"结构性"字段：method、path、参数定义、请求体定义、响应定义、安全需求。
 * 描述类字段（summary、description、tag 显示名、example 等）交由 content_localizations
 * 管理，不参与 hash，中心改写描述不会被误判为接口变更。
 */
public final class CanonicalHasher {

    /**
     * Hasher 版本号。逻辑变更（如字段纳入/剔除策略调整）时递增，
     * 使旧 hash 自然失效、重算。
     */
    public static final int VERSION = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);

    private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

    private CanonicalHasher() {
    }

    /**
     * 生成接口级的 SHA-256 hash（小写 hex）。
     */
    public static String hashOperation(CanonicalOperation operation) {
        ObjectNode node = toCanonicalNode(operation);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestOutputStream out = new DigestOutputStream(OutputStream.nullOutputStream(), digest);
                 JsonGenerator gen = MAPPER.getFactory().createGenerator(out)) {
                MAPPER.writeTree(gen, node);
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize canonical operation", e);
        }
    }

    /**
     * 生成接口定义的规范化 JSON 节点，用于 operation_definition_versions 存档。
     * 与 {@link #hashOperation(CanonicalOperation)} 共用同一 canonical 形态，
     * 保证 hash 与存储内容一致。
     */
    public static ObjectNode toCanonicalNode(CanonicalOperation operation) {
        ObjectNode root = NODES.objectNode();
        root.put("method", operation.getMethod().toUpperCase(Locale.ROOT));
        root.put("path", operation.getPath());

        ArrayNode params = root.putArray("parameters");
        for (CanonicalParameter p : sortParams(operation.getParameters())) {
            ObjectNode pn = NODES.objectNode();
            pn.put("location", p.getLocation());
            pn.put("name", p.getName());
            pn.put("required", p.isRequired());
            pn.set("schema", toSchemaNode(p.getSchema()));
            params.add(pn);
        }

        CanonicalRequestBody body = operation.getRequestBody();
        if (body != null) {
            ObjectNode bn = NODES.objectNode();
            bn.put("required", body.isRequired());
            if (body.getContentType() != null) {
                bn.put("contentType", body.getContentType());
            } else {
                bn.putNull("contentType");
            }
            bn.set("schema", toSchemaNode(body.getSchema()));
            root.set("requestBody", bn);
        } else {
            root.putNull("requestBody");
        }

        ArrayNode responses = root.putArray("responses");
        for (CanonicalResponse r : sortResponses(operation.getResponses())) {
            ObjectNode rn = NODES.objectNode();
            rn.put("statusCode", r.getStatusCode());
            if (r.getContentType() != null) {
                rn.put("contentType", r.getContentType());
            } else {
                rn.putNull("contentType");
            }
            rn.set("schema", toSchemaNode(r.getSchema()));
            responses.add(rn);
        }

        ArrayNode security = root.putArray("securityRequirements");
        if (operation.getSecurityRequirements() != null) {
            operation.getSecurityRequirements().stream().sorted().forEach(security::add);
        }

        return root;
    }

    private static List<CanonicalParameter> sortParams(List<CanonicalParameter> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        return input.stream()
                .sorted(Comparator.comparing(CanonicalParameter::getLocation)
                        .thenComparing(CanonicalParameter::getName))
                .toList();
    }

    private static List<CanonicalResponse> sortResponses(List<CanonicalResponse> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        return input.stream()
                .sorted(Comparator.comparing(CanonicalResponse::getStatusCode))
                .toList();
    }

    private static JsonNode toSchemaNode(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return NODES.nullNode();
        }
        return MAPPER.valueToTree(deepSortKeys(schema));
    }

    private static Object deepSortKeys(Object value) {
        if (value instanceof Map<?, ?> m) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) {
                sorted.put(String.valueOf(e.getKey()), deepSortKeys(e.getValue()));
            }
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(CanonicalHasher::deepSortKeys).toList();
        }
        return value;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
