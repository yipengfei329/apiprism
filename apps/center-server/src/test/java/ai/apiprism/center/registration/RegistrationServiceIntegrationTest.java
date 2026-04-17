package ai.apiprism.center.registration;

import ai.apiprism.center.catalog.CatalogService;
import ai.apiprism.center.localization.LocalizationEntity;
import ai.apiprism.center.localization.LocalizationField;
import ai.apiprism.center.localization.LocalizationRepository;
import ai.apiprism.center.localization.LocalizationSource;
import ai.apiprism.center.registration.persistence.OperationRepository;
import ai.apiprism.center.registration.persistence.OperationVersionRepository;
import ai.apiprism.center.registration.persistence.PersistenceRows.OperationRow;
import ai.apiprism.center.registration.persistence.ServiceRepository;
import ai.apiprism.model.CanonicalOperation;
import ai.apiprism.model.CanonicalServiceSnapshot;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ServiceDescriptor;
import ai.apiprism.protocol.registration.SpecPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 新规范化模型下 RegistrationService + CatalogService 的端到端验证。
 * 覆盖核心变化检测场景：fast-path、接口结构变化、仅描述变化、中心 locale 覆盖。
 */
@SpringBootTest
@ActiveProfiles("test")
class RegistrationServiceIntegrationTest {

    @Autowired RegistrationService registrationService;
    @Autowired CatalogService catalogService;
    @Autowired ServiceRepository serviceRepo;
    @Autowired OperationRepository opRepo;
    @Autowired OperationVersionRepository versionRepo;
    @Autowired LocalizationRepository localizationRepo;

    private static final String OPENAPI_V1 = """
            {
              "openapi": "3.0.3",
              "info": { "title": "Orders API", "version": "1.0.0" },
              "paths": {
                "/orders/{id}": {
                  "get": {
                    "operationId": "getOrder",
                    "tags": ["orders"],
                    "summary": "Fetch an order",
                    "description": "按订单号查询订单详情",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true,
                        "schema": { "type": "string" }, "description": "订单号" }
                    ],
                    "responses": {
                      "200": {
                        "description": "订单详情",
                        "content": { "application/json": { "schema": { "type": "object" } } }
                      }
                    }
                  }
                }
              }
            }
            """;

    private static final String OPENAPI_V1_DESC_ONLY = """
            {
              "openapi": "3.0.3",
              "info": { "title": "Orders API", "version": "1.0.0" },
              "paths": {
                "/orders/{id}": {
                  "get": {
                    "operationId": "getOrder",
                    "tags": ["orders"],
                    "summary": "抓取单个订单",
                    "description": "按订单号查询订单详细信息（改写后的描述）",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true,
                        "schema": { "type": "string" }, "description": "要查询的订单号" }
                    ],
                    "responses": {
                      "200": {
                        "description": "订单详情（改写）",
                        "content": { "application/json": { "schema": { "type": "object" } } }
                      }
                    }
                  }
                }
              }
            }
            """;

    private static final String OPENAPI_V2_SCHEMA = """
            {
              "openapi": "3.0.3",
              "info": { "title": "Orders API", "version": "2.0.0" },
              "paths": {
                "/orders/{id}": {
                  "get": {
                    "operationId": "getOrder",
                    "tags": ["orders"],
                    "summary": "Fetch an order",
                    "description": "按订单号查询订单详情",
                    "parameters": [
                      { "name": "id", "in": "path", "required": true,
                        "schema": { "type": "integer" }, "description": "订单号" }
                    ],
                    "responses": {
                      "200": { "description": "订单详情",
                        "content": { "application/json": { "schema": { "type": "object" } } } }
                    }
                  }
                }
              }
            }
            """;

    @Test
    void firstRegistration_createsFullNormalizedGraph() {
        register("orders", "dev", OPENAPI_V1);

        CanonicalServiceSnapshot snapshot = catalogService.getService("orders", "dev");
        assertEquals("Orders API", snapshot.getTitle());
        assertEquals("1.0.0", snapshot.getVersion());
        assertEquals(1, snapshot.getGroups().size());
        assertEquals("orders", snapshot.getGroups().get(0).getName());

        CanonicalOperation op = snapshot.getGroups().get(0).getOperations().get(0);
        assertEquals("getOrder", op.getOperationId());
        assertEquals("GET", op.getMethod());
        assertEquals("/orders/{id}", op.getPath());
        assertEquals("Fetch an order", op.getSummary());
        assertEquals("按订单号查询订单详情", op.getDescription());
        assertEquals(1, op.getParameters().size());
        assertEquals("订单号", op.getParameters().get(0).getDescription());
        assertEquals(1, op.getResponses().size());

        String serviceId = serviceRepo.findByName("orders").orElseThrow().id();
        OperationRow opRow = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();
        assertEquals(1, versionRepo.findByOperationId(opRow.id()).size(),
                "首次注册应写入一条 version 行");
    }

    @Test
    void reRegisteringSameSpec_isFastPath_noNewVersions() {
        register("orders-fast", "dev", OPENAPI_V1);
        String serviceId = serviceRepo.findByName("orders-fast").orElseThrow().id();
        OperationRow opRow = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();
        int versionsBefore = versionRepo.findByOperationId(opRow.id()).size();

        register("orders-fast", "dev", OPENAPI_V1);

        assertEquals(versionsBefore, versionRepo.findByOperationId(opRow.id()).size(),
                "相同 spec hash 不应进入结构比对路径");
    }

    @Test
    void descriptionOnlyChange_refreshesLocalization_butNoNewVersion() {
        register("orders-desc", "dev", OPENAPI_V1);
        String serviceId = serviceRepo.findByName("orders-desc").orElseThrow().id();
        OperationRow opRow = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();
        String hashBefore = opRow.definitionHash();
        int versionsBefore = versionRepo.findByOperationId(opRow.id()).size();

        register("orders-desc", "dev", OPENAPI_V1_DESC_ONLY);

        OperationRow after = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();
        assertEquals(hashBefore, after.definitionHash(), "仅描述变化不应触发 hash 变化");
        assertEquals(versionsBefore, versionRepo.findByOperationId(opRow.id()).size(),
                "仅描述变化不应追加 version 行");

        CanonicalOperation op = catalogService.getOperation("orders-desc", "dev", "getOrder");
        assertEquals("抓取单个订单", op.getSummary(), "adapter 提交的新摘要应立刻覆盖");
        assertEquals("按订单号查询订单详细信息（改写后的描述）", op.getDescription());
    }

    @Test
    void schemaChange_appendsNewVersion_andUpdatesStructure() {
        register("orders-schema", "dev", OPENAPI_V1);
        String serviceId = serviceRepo.findByName("orders-schema").orElseThrow().id();
        OperationRow opRow = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();
        String hashBefore = opRow.definitionHash();

        register("orders-schema", "dev", OPENAPI_V2_SCHEMA);

        OperationRow after = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();
        assertNotNull(after);
        assertTrue(!hashBefore.equals(after.definitionHash()), "参数类型变化应触发 hash 变化");
        assertEquals(2, versionRepo.findByOperationId(opRow.id()).size(),
                "结构变化应追加一条 version 行");
    }

    @Test
    void centerLocalization_overridesAdapter_forMatchingLocale() {
        register("orders-loc", "dev", OPENAPI_V1);
        String serviceId = serviceRepo.findByName("orders-loc").orElseThrow().id();
        OperationRow opRow = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();

        localizationRepo.upsert(LocalizationEntity.OPERATION, opRow.id(),
                LocalizationField.DESCRIPTION, "zh-CN", LocalizationSource.CENTER,
                "运营同学改写的中文描述");
        localizationRepo.upsert(LocalizationEntity.OPERATION, opRow.id(),
                LocalizationField.DESCRIPTION, "en-US", LocalizationSource.CENTER,
                "Operator-curated English description");

        CanonicalOperation zh = catalogService.getOperation("orders-loc", "dev", "getOrder", "zh-CN");
        assertEquals("运营同学改写的中文描述", zh.getDescription());

        CanonicalOperation en = catalogService.getOperation("orders-loc", "dev", "getOrder", "en-US");
        assertEquals("Operator-curated English description", en.getDescription());

        CanonicalOperation summaryZh = catalogService.getOperation("orders-loc", "dev", "getOrder", "zh-CN");
        assertEquals("Fetch an order", summaryZh.getSummary(),
                "未被中心覆盖的字段应保留 adapter 原值");
    }

    @Test
    void centerLocalization_survivesAdapterReRegistration() {
        register("orders-survive", "dev", OPENAPI_V1);
        String serviceId = serviceRepo.findByName("orders-survive").orElseThrow().id();
        OperationRow opRow = opRepo.findByServiceAndKey(serviceId, "getOrder").orElseThrow();

        localizationRepo.upsert(LocalizationEntity.OPERATION, opRow.id(),
                LocalizationField.DESCRIPTION, "zh-CN", LocalizationSource.CENTER,
                "不被覆盖的中心文案");

        register("orders-survive", "dev", OPENAPI_V1_DESC_ONLY);

        CanonicalOperation op = catalogService.getOperation("orders-survive", "dev", "getOrder", "zh-CN");
        assertEquals("不被覆盖的中心文案", op.getDescription(),
                "center 行永远优先于同 locale 的 adapter 行");
    }

    private void register(String serviceName, String env, String spec) {
        registrationService.register(ApiRegistrationRequest.builder()
                .service(ServiceDescriptor.builder()
                        .name(serviceName)
                        .environment(env)
                        .adapterType("test")
                        .build())
                .spec(SpecPayload.builder()
                        .format("openapi-json")
                        .content(spec)
                        .build())
                .build());
    }
}
