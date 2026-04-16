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

    private final ObjectProvider<OpenApiResource> openApiResourceProvider;
    // 懒加载反射方法引用，避免类加载时因 springdoc 版本不兼容抛异常导致整个配置类失败
    private volatile Method getOpenApiMethod;
    private volatile boolean methodResolved = false;

    public SpringDocOpenApiSupplier(ApiPrismProperties properties, ObjectProvider<OpenApiResource> openApiResourceProvider) {
        super(properties);
        this.openApiResourceProvider = openApiResourceProvider;
    }

    @Override
    public ApiPrismOpenApiDocument fetch(String localBaseUrl) {
        OpenApiResource openApiResource = openApiResourceProvider.orderedStream().findFirst().orElse(null);
        if (openApiResource != null) {
            Method method = resolveGetOpenApiMethod();
            if (method != null) {
                try {
                    return buildDocumentFromModel(loadInProcess(openApiResource, method), "springdoc-bean");
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    log.debug("Falling back to local OpenAPI HTTP fetch because in-process springdoc access failed: {}",
                            exception.getMessage());
                }
            }
        }
        return fetchViaHttp(localBaseUrl);
    }

    private OpenAPI loadInProcess(OpenApiResource openApiResource, Method method) throws ReflectiveOperationException {
        return (OpenAPI) method.invoke(openApiResource, Locale.getDefault());
    }

    private Method resolveGetOpenApiMethod() {
        if (methodResolved) {
            return getOpenApiMethod;
        }
        synchronized (this) {
            if (methodResolved) {
                return getOpenApiMethod;
            }
            try {
                Method method = AbstractOpenApiResource.class.getDeclaredMethod("getOpenApi", Locale.class);
                method.setAccessible(true);
                getOpenApiMethod = method;
            } catch (NoSuchMethodException exception) {
                log.debug("springdoc AbstractOpenApiResource.getOpenApi(Locale) not found, "
                        + "falling back to HTTP-based OpenAPI retrieval: {}", exception.getMessage());
                getOpenApiMethod = null;
            }
            methodResolved = true;
            return getOpenApiMethod;
        }
    }
}
