package ai.apiprism.adapter.starter.inspection;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PathPatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对比运行时 {@link RequestMappingHandlerMapping} 与 OpenAPI 文档中的路径，
 * 找出已映射但未文档化的端点，用于诊断和上报。
 */
public class ApiPrismMappingInspector {

    private static final String WILDCARD_METHOD = "*";

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;

    public ApiPrismMappingInspector(ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        this.handlerMappingProvider = handlerMappingProvider;
    }

    public ApiPrismRegistrationDiagnostics inspect(OpenAPI openApi) {
        RequestMappingHandlerMapping handlerMapping = handlerMappingProvider.getIfAvailable();
        if (handlerMapping == null || openApi == null) {
            return ApiPrismRegistrationDiagnostics.builder()
                    .mappingCount(0)
                    .documentedOperationCount(0)
                    .undocumentedMappings(List.of())
                    .build();
        }

        Set<ApiPrismEndpointMapping> runtimeMappings = collectRuntimeMappings(handlerMapping.getHandlerMethods());
        Set<ApiPrismEndpointMapping> documentedMappings = collectDocumentedMappings(openApi);

        List<String> undocumentedMappings = runtimeMappings.stream()
                .filter(mapping -> !isDocumented(mapping, documentedMappings))
                .sorted(Comparator.comparing(ApiPrismEndpointMapping::path).thenComparing(ApiPrismEndpointMapping::method))
                .map(ApiPrismEndpointMapping::display)
                .toList();

        return ApiPrismRegistrationDiagnostics.builder()
                .mappingCount(runtimeMappings.size())
                .documentedOperationCount(documentedMappings.size())
                .undocumentedMappings(undocumentedMappings)
                .build();
    }

    private Set<ApiPrismEndpointMapping> collectRuntimeMappings(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        Set<ApiPrismEndpointMapping> mappings = new LinkedHashSet<>();
        handlerMethods.forEach((info, handlerMethod) -> {
            if (shouldSkip(info, handlerMethod)) {
                return;
            }
            extractPaths(info).forEach(path -> {
                if (isFrameworkPath(path)) {
                    return;
                }
                Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
                if (methods.isEmpty()) {
                    mappings.add(new ApiPrismEndpointMapping(WILDCARD_METHOD, normalizePath(path)));
                    return;
                }
                methods.forEach(method -> mappings.add(new ApiPrismEndpointMapping(method.name(), normalizePath(path))));
            });
        });
        return mappings;
    }

    private boolean shouldSkip(RequestMappingInfo info, HandlerMethod handlerMethod) {
        Class<?> beanType = handlerMethod.getBeanType();
        if (isFrameworkController(beanType)) {
            return true;
        }
        if (AnnotatedElementUtils.hasAnnotation(beanType, Hidden.class)) {
            return true;
        }
        if (AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), Hidden.class)) {
            return true;
        }
        Operation operation = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), Operation.class);
        return operation != null && operation.hidden();
    }

    private boolean isFrameworkController(Class<?> beanType) {
        String className = beanType.getName();
        return className.startsWith("org.springdoc.")
                || className.startsWith("org.springframework.boot.actuate.")
                || className.equals("org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController");
    }

    private boolean isFrameworkPath(String path) {
        return path.equals("/error")
                || path.startsWith("/error/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/actuator");
    }

    private List<String> extractPaths(RequestMappingInfo info) {
        List<String> paths = new ArrayList<>();
        PathPatternsRequestCondition pathPatternsCondition = info.getPathPatternsCondition();
        if (pathPatternsCondition != null) {
            paths.addAll(pathPatternsCondition.getPatternValues());
        }
        if (paths.isEmpty() && info.getPatternsCondition() != null) {
            paths.addAll(info.getPatternsCondition().getPatterns());
        }
        return paths;
    }

    private Set<ApiPrismEndpointMapping> collectDocumentedMappings(OpenAPI openApi) {
        Set<ApiPrismEndpointMapping> mappings = new LinkedHashSet<>();
        if (openApi.getPaths() == null) {
            return mappings;
        }
        openApi.getPaths().forEach((path, pathItem) -> {
            if (pathItem == null) {
                return;
            }
            for (Map.Entry<PathItem.HttpMethod, io.swagger.v3.oas.models.Operation> entry : pathItem.readOperationsMap().entrySet()) {
                mappings.add(new ApiPrismEndpointMapping(entry.getKey().name(), normalizePath(path)));
            }
        });
        return mappings;
    }

    private boolean isDocumented(ApiPrismEndpointMapping runtimeMapping, Set<ApiPrismEndpointMapping> documentedMappings) {
        if (!WILDCARD_METHOD.equals(runtimeMapping.method())) {
            return documentedMappings.contains(runtimeMapping);
        }
        return documentedMappings.stream().anyMatch(documented -> documented.path().equals(runtimeMapping.path()));
    }

    private String normalizePath(String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return "/";
        }
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }
}
