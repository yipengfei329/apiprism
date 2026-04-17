package ai.apiprism.model.hash;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalParameter;
import ai.apiprism.model.CanonicalRequestBody;
import ai.apiprism.model.CanonicalResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalHasherTest {

    @Test
    void hash_isStable_whenOnlyDescriptionChanges() {
        CanonicalOperation a = baseOperation().toBuilder()
                .description("原始描述")
                .summary("原始摘要")
                .build();
        CanonicalOperation b = baseOperation().toBuilder()
                .description("改写后的描述")
                .summary("改写后的摘要")
                .build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenOnlyTagsChange() {
        CanonicalOperation a = baseOperation().toBuilder().tag("user").build();
        CanonicalOperation b = baseOperation().toBuilder().tag("account").tag("admin").build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenParameterDescriptionChanges() {
        CanonicalOperation a = baseOperation().toBuilder()
                .parameter(paramBuilder("id", "path").description("用户 ID").build())
                .build();
        CanonicalOperation b = baseOperation().toBuilder()
                .parameter(paramBuilder("id", "path").description("另一种说法的 ID").build())
                .build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenResponseDescriptionChanges() {
        CanonicalOperation a = baseOperation().toBuilder()
                .response(CanonicalResponse.builder()
                        .statusCode("200").contentType("application/json")
                        .description("成功").schema(Map.of("type", "string")).build())
                .build();
        CanonicalOperation b = baseOperation().toBuilder()
                .response(CanonicalResponse.builder()
                        .statusCode("200").contentType("application/json")
                        .description("操作成功返回").schema(Map.of("type", "string")).build())
                .build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenParameterInputOrderDiffers() {
        CanonicalParameter p1 = paramBuilder("id", "path").build();
        CanonicalParameter p2 = paramBuilder("X-Trace", "header").build();
        CanonicalParameter p3 = paramBuilder("page", "query").build();

        CanonicalOperation a = baseOperation().toBuilder()
                .parameter(p1).parameter(p2).parameter(p3).build();
        CanonicalOperation b = baseOperation().toBuilder()
                .parameter(p3).parameter(p1).parameter(p2).build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenResponseInputOrderDiffers() {
        CanonicalResponse r200 = CanonicalResponse.builder().statusCode("200")
                .contentType("application/json").schema(Map.of("type", "string")).build();
        CanonicalResponse r404 = CanonicalResponse.builder().statusCode("404")
                .contentType("application/json").schema(Map.of("type", "object")).build();

        CanonicalOperation a = baseOperation().toBuilder().response(r200).response(r404).build();
        CanonicalOperation b = baseOperation().toBuilder().response(r404).response(r200).build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenSchemaKeyOrderDiffers() {
        Map<String, Object> schemaA = new LinkedHashMap<>();
        schemaA.put("type", "object");
        schemaA.put("properties", Map.of("name", Map.of("type", "string"), "age", Map.of("type", "integer")));
        schemaA.put("required", List.of("name"));

        Map<String, Object> schemaB = new LinkedHashMap<>();
        schemaB.put("required", List.of("name"));
        schemaB.put("properties", Map.of("age", Map.of("type", "integer"), "name", Map.of("type", "string")));
        schemaB.put("type", "object");

        CanonicalOperation a = baseOperation().toBuilder()
                .parameter(paramBuilder("payload", "query").schema(schemaA).build()).build();
        CanonicalOperation b = baseOperation().toBuilder()
                .parameter(paramBuilder("payload", "query").schema(schemaB).build()).build();

        assertEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_isStable_whenMethodCaseDiffers() {
        CanonicalOperation upper = baseOperation().toBuilder().method("GET").build();
        CanonicalOperation lower = baseOperation().toBuilder().method("get").build();

        assertEquals(CanonicalHasher.hashOperation(upper), CanonicalHasher.hashOperation(lower));
    }

    @Test
    void hash_changes_whenParameterSchemaChanges() {
        CanonicalOperation a = baseOperation().toBuilder()
                .parameter(paramBuilder("id", "path").schema(Map.of("type", "string")).build()).build();
        CanonicalOperation b = baseOperation().toBuilder()
                .parameter(paramBuilder("id", "path").schema(Map.of("type", "integer")).build()).build();

        assertNotEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_changes_whenParameterRequiredFlagChanges() {
        CanonicalOperation a = baseOperation().toBuilder()
                .parameter(paramBuilder("id", "path").required(false).build()).build();
        CanonicalOperation b = baseOperation().toBuilder()
                .parameter(paramBuilder("id", "path").required(true).build()).build();

        assertNotEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_changes_whenResponseAdded() {
        CanonicalOperation a = baseOperation().toBuilder()
                .response(CanonicalResponse.builder().statusCode("200").build()).build();
        CanonicalOperation b = a.toBuilder()
                .response(CanonicalResponse.builder().statusCode("404").build()).build();

        assertNotEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_changes_whenPathChanges() {
        CanonicalOperation a = baseOperation().toBuilder().path("/users/{id}").build();
        CanonicalOperation b = baseOperation().toBuilder().path("/accounts/{id}").build();

        assertNotEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void hash_changes_whenRequestBodyRequiredChanges() {
        CanonicalOperation a = baseOperation().toBuilder()
                .requestBody(CanonicalRequestBody.builder()
                        .required(false).contentType("application/json")
                        .schema(Map.of("type", "object")).build()).build();
        CanonicalOperation b = baseOperation().toBuilder()
                .requestBody(CanonicalRequestBody.builder()
                        .required(true).contentType("application/json")
                        .schema(Map.of("type", "object")).build()).build();

        assertNotEquals(CanonicalHasher.hashOperation(a), CanonicalHasher.hashOperation(b));
    }

    @Test
    void canonicalNode_preservesFieldOrder() {
        ObjectNode node = CanonicalHasher.toCanonicalNode(baseOperation());
        List<String> fields = java.util.stream.StreamSupport
                .stream(java.util.Spliterators.spliteratorUnknownSize(node.fieldNames(), 0), false)
                .toList();
        assertEquals(List.of("method", "path", "parameters", "requestBody", "responses", "securityRequirements"), fields);
    }

    @Test
    void hashFormat_isLowercaseHex64() {
        String hash = CanonicalHasher.hashOperation(baseOperation());
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"), "hash should be lowercase hex: " + hash);
    }

    private static CanonicalOperation baseOperation() {
        return CanonicalOperation.builder()
                .operationId("getUser")
                .method("GET")
                .path("/users/{id}")
                .build();
    }

    private static CanonicalParameter.CanonicalParameterBuilder paramBuilder(String name, String location) {
        return CanonicalParameter.builder()
                .name(name)
                .location(location)
                .required(true)
                .schema(Map.of("type", "string"));
    }
}
