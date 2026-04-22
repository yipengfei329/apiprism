package ai.apiprism.openapi;

import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiNormalizerTest {

    private final OpenApiNormalizer normalizer = new OpenApiNormalizer();

    @Test
    void normalizesOperationsIntoGroupedCanonicalModel() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Prism Demo
                  version: 1.0.0
                tags:
                  - name: orders
                    description: Order operations
                paths:
                  /orders/{id}:
                    get:
                      tags: [orders]
                      operationId: getOrder
                      summary: Fetch an order
                      parameters:
                        - in: path
                          name: id
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: Success
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Order'
                components:
                  schemas:
                    Order:
                      type: object
                """;

        NormalizationResult result = normalizer.normalize("order-service", "dev", null, null, null, spec);

        assertEquals("Prism Demo", result.getSnapshot().getTitle());
        assertEquals(1, result.getSnapshot().getGroups().size());
        assertEquals("orders", result.getSnapshot().getGroups().getFirst().getName());
        assertEquals("getOrder", result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getOperationId());
        assertFalse(result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getParameters().isEmpty());

        // 验证参数 schema 为 JSON Schema map
        var param = result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getParameters().getFirst();
        assertNotNull(param.getSchema());
        assertEquals("string", param.getSchema().get("type"));
    }

    @Test
    void prefersRequestedServerUrlsOverSpecServers() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Prism Demo
                  version: 1.0.0
                servers:
                  - url: https://spec.example.com
                paths: {}
                """;

        NormalizationResult result = normalizer.normalize(
                "order-service",
                "dev",
                null,
                null,
                java.util.List.of("https://api-1.example.com", "https://api-2.example.com"),
                spec
        );

        assertEquals(
                java.util.List.of("https://api-1.example.com", "https://api-2.example.com"),
                result.getSnapshot().getServerUrls()
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractsObjectPropertiesAndRequiredList() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /users:
                    post:
                      operationId: createUser
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              type: object
                              required: [name, email]
                              properties:
                                name:
                                  type: string
                                  description: User name
                                email:
                                  type: string
                                  format: email
                                age:
                                  type: integer
                                  format: int32
                      responses:
                        '201':
                          description: Created
                """;

        NormalizationResult result = normalizer.normalize("user-service", "dev", null, null, null, spec);
        CanonicalOperation op = result.getSnapshot().getGroups().getFirst().getOperations().getFirst();

        Map<String, Object> schema = op.getRequestBody().getSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties);
        assertEquals(3, properties.size());

        Map<String, Object> nameSchema = (Map<String, Object>) properties.get("name");
        assertEquals("string", nameSchema.get("type"));
        assertEquals("User name", nameSchema.get("description"));

        Map<String, Object> emailSchema = (Map<String, Object>) properties.get("email");
        assertEquals("email", emailSchema.get("format"));

        List<String> required = (List<String>) schema.get("required");
        assertTrue(required.contains("name"));
        assertTrue(required.contains("email"));
        assertFalse(required.contains("age"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractsNestedRefObjects() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses:
                        '200':
                          description: Success
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Order'
                components:
                  schemas:
                    Order:
                      type: object
                      properties:
                        id:
                          type: integer
                          format: int64
                        customer:
                          $ref: '#/components/schemas/Customer'
                    Customer:
                      type: object
                      properties:
                        name:
                          type: string
                        email:
                          type: string
                """;

        NormalizationResult result = normalizer.normalize("order-service", "dev", null, null, null, spec);
        CanonicalResponse resp = result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getResponses().getFirst();

        Map<String, Object> schema = resp.getSchema();
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties.get("customer"));

        Map<String, Object> customer = (Map<String, Object>) properties.get("customer");
        Map<String, Object> customerProps = (Map<String, Object>) customer.get("properties");
        assertNotNull(customerProps);
        assertEquals("string", ((Map<String, Object>) customerProps.get("name")).get("type"));
        assertEquals("string", ((Map<String, Object>) customerProps.get("email")).get("type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void extractsArrayWithItemsRef() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /items:
                    get:
                      operationId: listItems
                      responses:
                        '200':
                          description: Success
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  $ref: '#/components/schemas/Item'
                components:
                  schemas:
                    Item:
                      type: object
                      properties:
                        sku:
                          type: string
                        price:
                          type: number
                          format: double
                """;

        NormalizationResult result = normalizer.normalize("item-service", "dev", null, null, null, spec);
        CanonicalResponse resp = result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getResponses().getFirst();

        Map<String, Object> schema = resp.getSchema();
        assertEquals("array", schema.get("type"));

        Map<String, Object> items = (Map<String, Object>) schema.get("items");
        assertNotNull(items);
        Map<String, Object> itemProps = (Map<String, Object>) items.get("properties");
        assertEquals("string", ((Map<String, Object>) itemProps.get("sku")).get("type"));
        assertEquals("double", ((Map<String, Object>) itemProps.get("price")).get("format"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void handlesCircularReferencesWithoutStackOverflow() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /tree:
                    get:
                      operationId: getTree
                      responses:
                        '200':
                          description: Success
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/TreeNode'
                components:
                  schemas:
                    TreeNode:
                      type: object
                      properties:
                        value:
                          type: string
                        children:
                          type: array
                          items:
                            $ref: '#/components/schemas/TreeNode'
                """;

        // 不应抛出 StackOverflowError
        NormalizationResult result = normalizer.normalize("tree-service", "dev", null, null, null, spec);
        CanonicalResponse resp = result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getResponses().getFirst();

        Map<String, Object> schema = resp.getSchema();
        assertNotNull(schema);
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertNotNull(properties.get("children"));

        Map<String, Object> children = (Map<String, Object>) properties.get("children");
        assertEquals("array", children.get("type"));

        // items 应该是循环引用标记，类型为引用名称
        Map<String, Object> items = (Map<String, Object>) children.get("items");
        assertNotNull(items);
        assertEquals(true, items.get("$circular"));
        assertEquals("TreeNode", items.get("type"));
    }

    @Test
    void deduplicatesDescriptionWhenItStartsWithSummary() {
        // 模拟 therapi + SpringDoc 的实际输出格式：summary/description 可能带 \n 和空格
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /notifications:
                    get:
                      operationId: listNotifications
                      summary: "查询用户通知列表。\n "
                      description: "查询用户通知列表。\n <p>\n 支持按通知类型、状态过滤，返回分页结果。"
                      responses:
                        '200':
                          description: Success
                    post:
                      operationId: sendNotification
                      summary: "Send notification"
                      description: "Send notification to target users, up to 1000 per request."
                      responses:
                        '201':
                          description: Created
                    delete:
                      operationId: deleteNotification
                      summary: "标记单条通知为已读。"
                      description: "标记单条通知为已读。"
                      responses:
                        '204':
                          description: Deleted
                    patch:
                      operationId: updateNotification
                      summary: "更新通知"
                      description: "更新通知内容和状态，支持部分字段修改。"
                      responses:
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("ntf-service", "dev", null, null, null, spec);
        var ops = result.getSnapshot().getGroups().getFirst().getOperations();

        // Javadoc 风格：description 以 summary 开头 + <p> 分隔，去重并去掉前导 <p>
        var listOp = ops.stream().filter(o -> o.getOperationId().equals("listNotifications")).findFirst().orElseThrow();
        assertEquals("支持按通知类型、状态过滤，返回分页结果。", listOp.getDescription());

        // 注解风格：summary 与 description 是不同句子，保持原样
        var sendOp = ops.stream().filter(o -> o.getOperationId().equals("sendNotification")).findFirst().orElseThrow();
        assertEquals("Send notification to target users, up to 1000 per request.", sendOp.getDescription());

        // summary 与 description 完全相同，description 应置空
        var deleteOp = ops.stream().filter(o -> o.getOperationId().equals("deleteNotification")).findFirst().orElseThrow();
        assertNull(deleteOp.getDescription());

        // summary 是 description 的前缀但后续不是 <p> 分隔（同一句延续），保持原样
        var updateOp = ops.stream().filter(o -> o.getOperationId().equals("updateNotification")).findFirst().orElseThrow();
        assertEquals("更新通知内容和状态，支持部分字段修改。", updateOp.getDescription());
    }

    @Test
    void generatesSlugForAsciiGroupName() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                tags:
                  - name: User Management
                paths:
                  /users:
                    get:
                      tags: [User Management]
                      operationId: listUsers
                      responses:
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("svc", "dev", null, null, null, spec);
        var group = result.getSnapshot().getGroups().getFirst();
        assertEquals("User Management", group.getName());
        assertEquals("user-management", group.getSlug());
    }

    @Test
    void generatesSlugForCjkGroupName() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                tags:
                  - name: 用户管理
                paths:
                  /users:
                    get:
                      tags: [用户管理]
                      operationId: listUsers
                      responses:
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("svc", "dev", null, null, null, spec);
        var group = result.getSnapshot().getGroups().getFirst();
        assertEquals("用户管理", group.getName());
        // slug 应为非空的 ASCII 字符串
        assertNotNull(group.getSlug());
        assertFalse(group.getSlug().isBlank());
        assertTrue(group.getSlug().matches("^[a-z0-9-]+$"));
    }

    @Test
    void deduplicatesSlugOnConflict() {
        // 两个不同的 tag 名但 slug 可能相同的场景，用 ASCII 名模拟
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                tags:
                  - name: orders
                  - name: Orders
                paths:
                  /orders:
                    get:
                      tags: [orders]
                      operationId: listOrders
                      responses:
                        '200':
                          description: OK
                  /orders/admin:
                    get:
                      tags: [Orders]
                      operationId: listOrdersAdmin
                      responses:
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("svc", "dev", null, null, null, spec);
        var groups = result.getSnapshot().getGroups();
        assertEquals(2, groups.size());
        // slug 不应重复
        assertNotEquals(groups.get(0).getSlug(), groups.get(1).getSlug());
    }

    @Test
    void extractsSimpleTypeParameterSchema() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /search:
                    get:
                      operationId: search
                      parameters:
                        - in: query
                          name: q
                          required: true
                          schema:
                            type: string
                        - in: query
                          name: limit
                          schema:
                            type: integer
                            format: int32
                      responses:
                        '200':
                          description: Success
                """;

        NormalizationResult result = normalizer.normalize("search-service", "dev", null, null, null, spec);
        var params = result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getParameters();

        assertEquals(2, params.size());

        assertEquals("string", params.get(0).getSchema().get("type"));
        assertNull(params.get(0).getSchema().get("format"));

        assertEquals("integer", params.get(1).getSchema().get("type"));
        assertEquals("int32", params.get(1).getSchema().get("format"));
    }

    @Test
    void sortsResponsesByStatusCodeAscending() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Test
                  version: 1.0.0
                paths:
                  /orders:
                    get:
                      operationId: listOrders
                      responses:
                        '404':
                          description: Not Found
                        default:
                          description: Fallback
                        '201':
                          description: Created
                        '2XX':
                          description: Success range
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("orders-service", "dev", null, null, null, spec);
        var responses = result.getSnapshot().getGroups().getFirst().getOperations().getFirst().getResponses();

        assertEquals(List.of("200", "201", "2XX", "404", "default"),
                responses.stream().map(CanonicalResponse::getStatusCode).toList());
    }

    @Test
    void inheritsGlobalSecurityWhenOperationHasNone() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Secure API
                  version: 1.0.0
                security:
                  - bearerAuth: []
                components:
                  securitySchemes:
                    bearerAuth:
                      type: http
                      scheme: bearer
                paths:
                  /users:
                    get:
                      operationId: listUsers
                      responses:
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("secure-svc", "prod", null, null, null, spec);
        CanonicalOperation op = result.getSnapshot().getGroups().getFirst().getOperations().getFirst();
        assertEquals(List.of("bearerAuth"), op.getSecurityRequirements());
    }

    @Test
    void operationLevelSecurityOverridesGlobal() {
        String spec = """
                openapi: 3.0.1
                info:
                  title: Secure API
                  version: 1.0.0
                security:
                  - globalAuth: []
                components:
                  securitySchemes:
                    globalAuth:
                      type: http
                      scheme: bearer
                    opAuth:
                      type: apiKey
                      in: header
                      name: X-API-Key
                paths:
                  /public:
                    get:
                      operationId: publicEndpoint
                      security: []
                      responses:
                        '200':
                          description: OK
                  /private:
                    get:
                      operationId: privateEndpoint
                      security:
                        - opAuth: []
                      responses:
                        '200':
                          description: OK
                """;

        NormalizationResult result = normalizer.normalize("secure-svc", "prod", null, null, null, spec);
        var ops = result.getSnapshot().getGroups().stream()
                .flatMap(g -> g.getOperations().stream())
                .collect(java.util.stream.Collectors.toMap(CanonicalOperation::getOperationId, o -> o));

        // 显式 security: [] 表示该接口无需认证
        assertEquals(List.of(), ops.get("publicEndpoint").getSecurityRequirements());
        // 显式 security 覆盖全局
        assertEquals(List.of("opAuth"), ops.get("privateEndpoint").getSecurityRequirements());
    }
}
