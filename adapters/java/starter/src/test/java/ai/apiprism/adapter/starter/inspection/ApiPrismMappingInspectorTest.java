package ai.apiprism.adapter.starter.inspection;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link ApiPrismMappingInspector} 在多 {@link RequestMappingHandlerMapping} Bean 场景下的兼容性，
 * 例如 Actuator 共存时容器中存在多个 HandlerMapping 实例。
 */
class ApiPrismMappingInspectorTest {

    @SuppressWarnings("unchecked")
    @Test
    void collectsMappingsFromMultipleHandlerMappingBeans() throws NoSuchMethodException {
        // 模拟主 RequestMappingHandlerMapping（用户业务 Controller）
        RequestMappingHandlerMapping primaryMapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo userMappingInfo = RequestMappingInfo
                .paths("/api/users")
                .methods(RequestMethod.GET)
                .build();
        HandlerMethod userHandler = new HandlerMethod(new SampleController(), "listUsers");
        when(primaryMapping.getHandlerMethods()).thenReturn(Map.of(userMappingInfo, userHandler));

        // 模拟第二个 RequestMappingHandlerMapping（例如 Actuator 注册的 controllerEndpointHandlerMapping）
        RequestMappingHandlerMapping secondaryMapping = mock(RequestMappingHandlerMapping.class);
        RequestMappingInfo orderMappingInfo = RequestMappingInfo
                .paths("/api/orders")
                .methods(RequestMethod.POST)
                .build();
        HandlerMethod orderHandler = new HandlerMethod(new SampleController(), "createOrder");
        when(secondaryMapping.getHandlerMethods()).thenReturn(Map.of(orderMappingInfo, orderHandler));

        ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.of(primaryMapping, secondaryMapping));

        ApiPrismMappingInspector inspector = new ApiPrismMappingInspector(provider);

        OpenAPI openApi = new OpenAPI();
        Paths paths = new Paths();
        PathItem usersPath = new PathItem();
        usersPath.setGet(new io.swagger.v3.oas.models.Operation());
        paths.addPathItem("/api/users", usersPath);
        PathItem ordersPath = new PathItem();
        ordersPath.setPost(new io.swagger.v3.oas.models.Operation());
        paths.addPathItem("/api/orders", ordersPath);
        openApi.setPaths(paths);

        ApiPrismRegistrationDiagnostics diagnostics = inspector.inspect(openApi);

        // 两个 HandlerMapping 的映射都被收集
        assertEquals(2, diagnostics.getMappingCount());
        assertEquals(2, diagnostics.getDocumentedOperationCount());
        assertEquals(0, diagnostics.getUndocumentedMappings().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void returnsEmptyDiagnosticsWhenNoHandlerMappingAvailable() {
        ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.orderedStream()).thenReturn(Stream.empty());

        ApiPrismMappingInspector inspector = new ApiPrismMappingInspector(provider);

        OpenAPI openApi = new OpenAPI();
        openApi.setPaths(new Paths());

        ApiPrismRegistrationDiagnostics diagnostics = inspector.inspect(openApi);

        assertEquals(0, diagnostics.getMappingCount());
        assertEquals(0, diagnostics.getDocumentedOperationCount());
    }

    /** 测试用 Controller */
    static class SampleController {
        @SuppressWarnings("unused")
        public String listUsers() { return "users"; }
        @SuppressWarnings("unused")
        public String createOrder() { return "order"; }
    }
}
