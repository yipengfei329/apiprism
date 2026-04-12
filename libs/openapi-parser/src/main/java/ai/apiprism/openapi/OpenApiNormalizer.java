package ai.apiprism.openapi;

import ai.apiprism.openapi.exceptions.NormalizationException;
import ai.apiprism.model.CanonicalGroup;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
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
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class OpenApiNormalizer {

    private static final int MAX_SCHEMA_DEPTH = 8;

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

        if (openApi.getPaths() != null) {
            openApi.getPaths().forEach((path, pathItem) -> {
                for (Map.Entry<PathItem.HttpMethod, Operation> entry : pathItem.readOperationsMap().entrySet()) {
                    Operation operation = entry.getValue();
                    String groupName = Optional.ofNullable(operation.getTags())
                            .filter(tags -> !tags.isEmpty())
                            .map(tags -> tags.getFirst())
                            .orElse("default");
                    groupedOperations.computeIfAbsent(groupName, ignored -> new ArrayList<>())
                            .add(toOperation(path, pathItem, entry.getKey(), operation));
                }
            });
        }

        List<CanonicalGroup> groups = groupedOperations.entrySet().stream()
                .map(entry -> CanonicalGroup.builder()
                        .name(entry.getKey())
                        .description(tagDescriptions.get(entry.getKey()))
                        .operations(entry.getValue().stream()
                                .sorted(Comparator.comparing(CanonicalOperation::getPath).thenComparing(CanonicalOperation::getMethod))
                                .toList())
                        .build())
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

    private CanonicalOperation toOperation(String path, PathItem pathItem, PathItem.HttpMethod method, Operation operation) {
        String operationId = firstNonBlank(operation.getOperationId(), fallbackOperationId(method, path));
        List<CanonicalParameter> parameters = mergeParameters(pathItem, operation).stream()
                .map(this::toParameter)
                .toList();
        CanonicalRequestBody requestBody = toRequestBody(operation);
        List<CanonicalResponse> responses = Optional.ofNullable(operation.getResponses())
                .map(apiResponses -> apiResponses.entrySet().stream().map(this::toResponse).toList())
                .orElseGet(List::of);
        List<String> securityRequirements = Optional.ofNullable(operation.getSecurity())
                .orElseGet(List::of)
                .stream()
                .flatMap(requirement -> requirement.keySet().stream())
                .distinct()
                .toList();

        return CanonicalOperation.builder()
                .operationId(operationId)
                .method(method.name())
                .path(path)
                .summary(operation.getSummary())
                .description(operation.getDescription())
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

    private String fallbackOperationId(PathItem.HttpMethod method, String path) {
        return method.name().toLowerCase(Locale.ROOT) + "_" + path
                .replace('/', '_')
                .replace('{', '_')
                .replace('}', '_')
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
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
