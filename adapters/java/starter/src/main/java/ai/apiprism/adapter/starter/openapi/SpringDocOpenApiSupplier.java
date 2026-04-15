package ai.apiprism.adapter.starter.openapi;

import ai.apiprism.adapter.starter.ApiPrismProperties;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.webmvc.api.OpenApiResource;
import org.springframework.beans.factory.ObjectProvider;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * {@link ApiPrismOpenApiSupplier} 的 SpringDoc 实现。
 * <p>
 * 优先通过反射直接从 SpringDoc Bean 读取 OpenAPI 模型（进程内，避免额外 HTTP 请求）；
 * 若 Bean 不可用或反射失败，则回退到本地 HTTP 请求指定的 OpenAPI 路径。
 * <p>
 * 本类仅在 SpringDoc 位于类路径时由自动配置激活（{@code @ConditionalOnClass}），
 * 不在类路径时 starter 回退到父类 {@link HttpOpenApiSupplier}。
 */
public class SpringDocOpenApiSupplier extends HttpOpenApiSupplier {

    private static final Logger log = LoggerFactory.getLogger(SpringDocOpenApiSupplier.class);
    private static final Method GET_OPEN_API_METHOD = initGetOpenApiMethod();

    private final ObjectProvider<OpenApiResource> openApiResourceProvider;

    public SpringDocOpenApiSupplier(ApiPrismProperties properties, ObjectProvider<OpenApiResource> openApiResourceProvider) {
        super(properties);
        this.openApiResourceProvider = openApiResourceProvider;
    }

    @Override
    public ApiPrismOpenApiDocument fetch(String localBaseUrl) {
        OpenApiResource openApiResource = openApiResourceProvider.orderedStream().findFirst().orElse(null);
        if (openApiResource != null) {
            try {
                return buildDocumentFromModel(loadInProcess(openApiResource), "springdoc-bean");
            } catch (ReflectiveOperationException | RuntimeException exception) {
                log.debug("Falling back to local OpenAPI HTTP fetch because in-process springdoc access failed: {}",
                        exception.getMessage());
            }
        }
        return fetchViaHttp(localBaseUrl);
    }

    private OpenAPI loadInProcess(OpenApiResource openApiResource) throws ReflectiveOperationException {
        return (OpenAPI) GET_OPEN_API_METHOD.invoke(openApiResource, Locale.getDefault());
    }

    private static Method initGetOpenApiMethod() {
        try {
            Method method = AbstractOpenApiResource.class.getDeclaredMethod("getOpenApi", Locale.class);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Unable to access springdoc OpenAPI builder.", exception);
        }
    }
}
