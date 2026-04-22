package ai.apiprism.openapi;

import ai.apiprism.openapi.exceptions.NormalizationException;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOAuthFlow;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import ai.apiprism.model.CanonicalSecurityScheme;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.model.ServiceRef;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import com.github.slugify.Slugify;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class OpenApiNormalizer {

    /**
     * 规范化逻辑版本号。
     * 当 normalize 产出的结构因逻辑变更而不同于旧版本时递增此值，
     * 使注册端的 specHash 失效，强制重写已持久化的 snapshot。
     */
    public static final int VERSION = 4;

    private static final int MAX_SCHEMA_DEPTH = 8;

    private static final Slugify SLUGIFY = Slugify.builder()
            .transliterator(true)
            .lowerCase(true)
            .build();

    /**
     * 将任意文本转为 URL 安全的 slug。
     * 供外部模块为旧数据补全缺失的 slug 使用。
     */
    public static String slugify(String text) {
        return SLUGIFY.slugify(text);
    }

    public NormalizationResult normalize(
            String serviceName,
            String environment,
            String requestedTitle,
            String requestedVersion,
            List<String> requestedServerUrls,
            String rawSpec
    ) {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setFlatten(false);

        SwaggerParseResult parseResult = new OpenAPIParser().readContents(rawSpec, null, options);
        OpenAPI openApi = parseResult.getOpenAPI();
        if (openApi == null) {
            throw new NormalizationException("Unable to parse OpenAPI content.");
        }

        List<String> warnings = Optional.ofNullable(parseResult.getMessages()).orElseGet(List::of);
        Map<String, String> tagDescriptions = extractTagDescriptions(openApi);
        Map<String, List<CanonicalOperation>> groupedOperations = new LinkedHashMap<>();

        // 文档级全局 security，当 operation 未定义自己的 security 时作为 fallback
        List<SecurityRequirement> globalSecurity = Optional.ofNullable(openApi.getSecurity()).orElseGet(List::of);

        if (openApi.getPaths() != null) {
            openApi.getPaths().forEach((path, pathItem) -> {
                for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
                    Operation operation = entry.getValue();
                    String groupName = Optional.ofNullable(operation.getTags())
                            .filter(tags -> !tags.isEmpty())
                            .map(tags -> tags.getFirst())
                            .orElse("default");
                    groupedOperations.computeIfAbsent(groupName, ignored -> new ArrayList<>())
                            .add(toOperation(path, pathItem, entry.getKey(), operation, globalSecurity));
                }
            });
        }

        // 生成 slug 并处理同服务内 slug 冲突
        Set<String> usedSlugs = new HashSet<>();
        List<CanonicalGroup> groups = groupedOperations.entrySet().stream()
                .map(entry -> {
                    String slug = deduplicateSlug(SLUGIFY.slugify(entry.getKey()), usedSlugs);
                    usedSlugs.add(slug);
                    return CanonicalGroup.builder()
                            .name(entry.getKey())
                            .slug(slug)
                            .description(tagDescriptions.get(entry.getKey()))
                            .operations(entry.getValue().stream()
                                    .sorted(Comparator.comparing(CanonicalOperation::getPath).thenComparing(CanonicalOperation::getMethod))
                                    .toList())
                            .build();
                })
                .toList();

        String title = firstNonBlank(requestedTitle, Optional.ofNullable(openApi.getInfo()).map(info -> info.getTitle()).orElse(null), serviceName);
        String version = firstNonBlank(requestedVersion, Optional.ofNullable(openApi.getInfo()).map(info -> info.getVersion()).orElse(null), "unspecified");
        List<String> specServerUrls = Optional.ofNullable(openApi.getServers())
                .orElseGet(List::of)
                .stream()
                .map(server -> server.getUrl())
                .filter(Objects::nonNull)
                .toList();
        List<String> serverUrls = Optional.ofNullable(requestedServerUrls)
                .orElseGet(List::of)
                .stream()
                .filter(Objects::nonNull)
                .filter(url -> !url.isBlank())
                .toList();
        if (serverUrls.isEmpty()) {
            serverUrls = specServerUrls;
        }

        CanonicalServiceSnapshot snapshot = CanonicalServiceSnapshot.builder()
                .ref(ServiceRef.builder()
                        .name(serviceName)
                        .environment(environment)
                        .build())
                .title(title)
                .version(version)
                .serverUrls(serverUrls)
                .groups(groups)
                .securitySchemes(extractSecuritySchemes(openApi))
                .updatedAt(Instant.now())
                .build();
        return NormalizationResult.builder()
                .snapshot(snapshot)
                .warnings(warnings)
                .build();
    }

    private Map<String, String> extractTagDescriptions(OpenAPI openApi) {
        Map<String, String> descriptions = new LinkedHashMap<>();
        Optional.ofNullable(openApi.getTags()).orElseGet(List::of).forEach(tag -> descriptions.put(tag.getName(), tag.getDescription()));
        return descriptions;
    }

    private CanonicalOperation toOperation(String path, PathItem pathItem, PathItem.HttpMethod method,
                                            Operation operation, List<SecurityRequirement> globalSecurity) {
        String operationId = firstNonBlank(operation.getOperationId(), fallbackOperationId(method, path));
        List<CanonicalParameter> parameters = mergeParameters(pathItem, operation).stream()
                .map(this::toParameter)
                .toList();
        CanonicalRequestBody requestBody = toRequestBody(operation);
        List<CanonicalResponse> responses = Optional.ofNullable(operation.getResponses())
                .map(apiResponses -> apiResponses.entrySet().stream()
                        .map(this::toResponse)
                        .sorted(Comparator.comparingInt(response -> responseSortKey(response.getStatusCode())))
                        .toList())
                .orElseGet(List::of);
        // operation.getSecurity() == null → 继承文档级全局声明
        // operation.getSecurity() == 空列表 [] → 显式声明该接口不需要认证
        List<SecurityRequirement> effectiveSecurity = operation.getSecurity() != null
                ? operation.getSecurity()
                : globalSecurity;
        List<String> securityRequirements = effectiveSecurity.stream()
                .flatMap(requirement -> requirement.keySet().stream())
                .distinct()
                .toList();

        return CanonicalOperation.builder()
                .operationId(operationId)
                .method(method.name())
                .path(path)
                .summary(stripToNull(operation.getSummary()))
                .description(deduplicateDescription(operation.getSummary(), operation.getDescription()))
                .tags(Optional.ofNullable(operation.getTags()).orElseGet(List::of))
                .securityRequirements(securityRequirements)
                .parameters(parameters)
                .requestBody(requestBody)
                .responses(responses)
                .build();
    }

    private List<Parameter> mergeParameters(PathItem pathItem, Operation operation) {
        List<Parameter> parameters = new ArrayList<>();
        parameters.addAll(Optional.ofNullable(pathItem.getParameters()).orElseGet(List::of));
        parameters.addAll(Optional.ofNullable(operation.getParameters()).orElseGet(List::of));
        return parameters;
    }

    private CanonicalParameter toParameter(Parameter parameter) {
        return CanonicalParameter.builder()
                .name(parameter.getName())
                .location(parameter.getIn())
                .required(Boolean.TRUE.equals(parameter.getRequired()))
                .schema(schemaToMap(parameter.getSchema()))
                .description(parameter.getDescription())
                .build();
    }

    private CanonicalRequestBody toRequestBody(Operation operation) {
        if (operation.getRequestBody() == null || operation.getRequestBody().getContent() == null || operation.getRequestBody().getContent().isEmpty()) {
            return null;
        }

        Map.Entry<String, MediaType> entry = operation.getRequestBody().getContent().entrySet().iterator().next();
        return CanonicalRequestBody.builder()
                .required(Boolean.TRUE.equals(operation.getRequestBody().getRequired()))
                .contentType(entry.getKey())
                .schema(schemaToMap(entry.getValue().getSchema()))
                .build();
    }

    private CanonicalResponse toResponse(Map.Entry<String, ApiResponse> entry) {
        ApiResponse apiResponse = entry.getValue();
        Content content = apiResponse.getContent();
        if (content == null || content.isEmpty()) {
            return CanonicalResponse.builder()
                    .statusCode(entry.getKey())
                    .description(apiResponse.getDescription())
                    .build();
        }

        Map.Entry<String, MediaType> media = content.entrySet().iterator().next();
        return CanonicalResponse.builder()
                .statusCode(entry.getKey())
                .description(apiResponse.getDescription())
                .contentType(media.getKey())
                .schema(schemaToMap(media.getValue().getSchema()))
                .build();
    }

    private int responseSortKey(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return Integer.MAX_VALUE - 1;
        }

        String normalized = statusCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.matches("\\d{3}")) {
            return Integer.parseInt(normalized);
        }
        if (normalized.matches("[1-5]XX")) {
            return (normalized.charAt(0) - '0') * 100 + 99;
        }
        if ("DEFAULT".equals(normalized)) {
            return Integer.MAX_VALUE - 2;
        }
        return Integer.MAX_VALUE;
    }

    /**
     * 将 swagger-parser 的 Schema 对象转为标准 JSON Schema Map。
     * 配合 setResolveFully(true) 使用：非循环 $ref 已内联，循环 $ref 保留原始引用。
     */
    private Map<String, Object> schemaToMap(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        return schemaToMap(schema, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> schemaToMap(Schema<?> schema, Set<Schema<?>> visited, int depth) {
        if (schema == null) {
            return null;
        }
        // 循环引用：resolveFully 会保留循环 $ref 不内联
        if (schema.get$ref() != null) {
            Map<String, Object> circular = new LinkedHashMap<>();
            String refName = schema.get$ref().substring(schema.get$ref().lastIndexOf('/') + 1);
            circular.put("type", refName);
            circular.put("$circular", true);
            return circular;
        }
        // 超出深度限制，只保留类型摘要
        if (depth > MAX_SCHEMA_DEPTH) {
            Map<String, Object> stub = new LinkedHashMap<>();
            stub.put("type", schema.getType() != null ? schema.getType() : "object");
            return stub;
        }
        // 安全网：同一对象在当前路径上重复出现
        if (!visited.add(schema)) {
            Map<String, Object> circular = new LinkedHashMap<>();
            circular.put("type", schema.getType() != null ? schema.getType() : "object");
            circular.put("$circular", true);
            return circular;
        }
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            if (schema.getType() != null) map.put("type", schema.getType());
            if (schema.getFormat() != null) map.put("format", schema.getFormat());
            if (schema.getDescription() != null) map.put("description", schema.getDescription());
            if (Boolean.TRUE.equals(schema.getDeprecated())) map.put("deprecated", true);
            if (schema.getExample() != null) map.put("example", schema.getExample());
            if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
                map.put("enum", schema.getEnum());
            }
            // 对象属性
            if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
                Map<String, Object> props = new LinkedHashMap<>();
                schema.getProperties().forEach((name, propSchema) ->
                        props.put(name, schemaToMap((Schema<?>) propSchema, visited, depth + 1)));
                map.put("properties", props);
            }
            // required 列表
            if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
                map.put("required", List.copyOf(schema.getRequired()));
            }
            // 数组元素
            if (schema instanceof ArraySchema arr && arr.getItems() != null) {
                map.put("items", schemaToMap(arr.getItems(), visited, depth + 1));
            }
            return map;
        } finally {
            visited.remove(schema); // 回溯，允许同一 schema 在不同分支复用
        }
    }

    /**
     * 去除 description 中与 summary 重复的前缀。
     * <p>
     * Javadoc 经 therapi + SpringDoc 处理后，实际输出格式为：
     * <ul>
     *   <li>summary: {@code "查询用户通知列表。\n "} — 尾部带换行+空格</li>
     *   <li>description: {@code "查询用户通知列表。\n <p>\n 支持按..."} — summary 原文 + 段落分隔 + 后续内容</li>
     * </ul>
     * 规则：先对两端 strip，再判断 description 是否以 summary 开头；
     * 若是，仅在后续为空或以 HTML 段落标签/空行分隔时去重，避免误裁同一句的延续。
     */
    private String deduplicateDescription(String summary, String description) {
        if (summary == null || summary.isBlank() || description == null || description.isBlank()) {
            return description;
        }
        // therapi 生成的文本两端可能有 \n 和空格，统一 strip 后比较
        String normSummary = summary.strip();
        String normDesc = description.strip();
        if (!normDesc.startsWith(normSummary)) {
            return description;
        }
        String tail = normDesc.substring(normSummary.length()).strip();
        // 完全相同 → 描述无额外信息
        if (tail.isEmpty()) {
            return null;
        }
        // summary 后必须以 HTML 段落标签分隔，才认定为 Javadoc 风格的重复前缀
        if (tail.matches("^<[pP]>[\\s\\S]*")) {
            String remaining = tail.replaceFirst("^<[pP]>\\s*", "").strip();
            return remaining.isEmpty() ? null : remaining;
        }
        // 非段落分隔（如同一句延续），保持原样
        return description;
    }

    /**
     * 提取 OpenAPI components.securitySchemes 为归一化模型。
     */
    private Map<String, CanonicalSecurityScheme> extractSecuritySchemes(OpenAPI openApi) {
        var rawSchemes = Optional.ofNullable(openApi.getComponents())
                .map(c -> c.getSecuritySchemes())
                .orElse(null);
        if (rawSchemes == null || rawSchemes.isEmpty()) {
            return Map.of();
        }
        Map<String, CanonicalSecurityScheme> result = new LinkedHashMap<>();
        rawSchemes.forEach((name, scheme) -> {
            if (scheme == null) return;
            result.put(name, CanonicalSecurityScheme.builder()
                    .type(scheme.getType() != null ? scheme.getType().toString().toLowerCase() : null)
                    .scheme(scheme.getScheme())
                    .bearerFormat(scheme.getBearerFormat())
                    .in(scheme.getIn() != null ? scheme.getIn().toString().toLowerCase() : null)
                    .paramName(scheme.getName())
                    .openIdConnectUrl(scheme.getOpenIdConnectUrl())
                    .description(scheme.getDescription())
                    .oauthFlows(extractOAuthFlows(scheme.getFlows()))
                    .build());
        });
        return result;
    }

    private List<CanonicalOAuthFlow> extractOAuthFlows(OAuthFlows flows) {
        if (flows == null) return List.of();
        List<CanonicalOAuthFlow> result = new ArrayList<>();
        if (flows.getImplicit() != null) result.add(toOAuthFlow("implicit", flows.getImplicit()));
        if (flows.getPassword() != null) result.add(toOAuthFlow("password", flows.getPassword()));
        if (flows.getClientCredentials() != null) result.add(toOAuthFlow("clientCredentials", flows.getClientCredentials()));
        if (flows.getAuthorizationCode() != null) result.add(toOAuthFlow("authorizationCode", flows.getAuthorizationCode()));
        return result;
    }

    private CanonicalOAuthFlow toOAuthFlow(String flowType, OAuthFlow flow) {
        return CanonicalOAuthFlow.builder()
                .flowType(flowType)
                .authorizationUrl(flow.getAuthorizationUrl())
                .tokenUrl(flow.getTokenUrl())
                .refreshUrl(flow.getRefreshUrl())
                .scopes(flow.getScopes() != null ? new LinkedHashMap<>(flow.getScopes()) : Map.of())
                .build();
    }

    /**
     * slug 冲突时追加数字后缀。
     */
    private String deduplicateSlug(String slug, Set<String> usedSlugs) {
        if (!usedSlugs.contains(slug)) {
            return slug;
        }
        for (int i = 2; ; i++) {
            String candidate = slug + "-" + i;
            if (!usedSlugs.contains(candidate)) {
                return candidate;
            }
        }
    }

    private String fallbackOperationId(PathItem.HttpMethod method, String path) {
        return method.name().toLowerCase(Locale.ROOT) + "_" + path
                .replace('/', '_')
                .replace('{', '_')
                .replace('}', '_')
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private String stripToNull(String value) {
        if (value == null) return null;
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
