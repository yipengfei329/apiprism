package ai.apiprism.adapter.starter.springdoc;

import ai.apiprism.adapter.starter.ApiHidden;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 在 Springdoc 生成 OpenAPI spec 时，自动过滤掉不应暴露给 API 消费者的参数。
 *
 * <p>过滤规则（满足其一即排除）：
 * <ol>
 *   <li>参数直接标注了 {@link ApiHidden}；</li>
 *   <li>参数上的某个注解本身以 {@link ApiHidden} 为元注解（例如 {@code @SessionUser}）；</li>
 *   <li>参数类型属于内置排除列表（Spring MVC 框架注入的非 HTTP 参数）；</li>
 *   <li>参数类型全限定名在用户配置的 {@code apiprism.excluded-parameter-types} 列表中。</li>
 * </ol>
 */
public class ApiPrismOperationCustomizer implements OperationCustomizer {

    private static final Logger log = LoggerFactory.getLogger(ApiPrismOperationCustomizer.class);

    /** Spring MVC 框架自身注入的非 HTTP 参数类型，对 API 消费者没有意义。 */
    private static final Set<String> BUILT_IN_EXCLUDED_TYPES = Set.of(
            "jakarta.servlet.http.HttpServletRequest",
            "jakarta.servlet.http.HttpServletResponse",
            "jakarta.servlet.http.HttpSession",
            "javax.servlet.http.HttpServletRequest",
            "javax.servlet.http.HttpServletResponse",
            "javax.servlet.http.HttpSession",
            "java.security.Principal",
            "org.springframework.security.core.Authentication",
            "org.springframework.security.core.userdetails.UserDetails",
            "org.springframework.web.context.request.WebRequest",
            "org.springframework.web.context.request.NativeWebRequest",
            "org.springframework.validation.BindingResult",
            "org.springframework.validation.Errors",
            "org.springframework.ui.Model",
            "org.springframework.ui.ModelMap"
    );

    private final Set<String> userExcludedTypeNames;

    public ApiPrismOperationCustomizer(List<String> excludedTypes) {
        this.userExcludedTypeNames = (excludedTypes != null && !excludedTypes.isEmpty())
                ? Collections.unmodifiableSet(new HashSet<>(excludedTypes))
                : Collections.emptySet();
    }

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (operation == null || operation.getParameters() == null || operation.getParameters().isEmpty()) {
            return operation;
        }

        Set<String> namesToRemove = collectExcludedParamNames(handlerMethod);
        if (namesToRemove.isEmpty()) {
            return operation;
        }

        List<Parameter> filtered = operation.getParameters().stream()
                .filter(p -> p.getName() == null || !namesToRemove.contains(p.getName()))
                .toList();

        log.debug("APIPrism filtered {} hidden parameter(s) {} from operation '{}'",
                operation.getParameters().size() - filtered.size(),
                namesToRemove,
                operation.getOperationId());

        operation.setParameters(filtered.isEmpty() ? null : filtered);
        return operation;
    }

    private Set<String> collectExcludedParamNames(HandlerMethod handlerMethod) {
        Set<String> names = new HashSet<>();
        for (MethodParameter mp : handlerMethod.getMethodParameters()) {
            if (shouldExclude(mp)) {
                addParameterName(mp, names);
            }
        }
        return names;
    }

    private boolean shouldExclude(MethodParameter param) {
        // 1. 直接标注 @ApiHidden
        if (param.hasParameterAnnotation(ApiHidden.class)) {
            return true;
        }
        // 2. 参数上的注解以 @ApiHidden 为元注解（如 @SessionUser 本身带有 @ApiHidden）
        for (Annotation ann : param.getParameterAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(ApiHidden.class)) {
                return true;
            }
        }
        // 3. 参数类型在内置或用户配置的排除列表中
        String typeName = param.getParameterType().getName();
        return BUILT_IN_EXCLUDED_TYPES.contains(typeName) || userExcludedTypeNames.contains(typeName);
    }

    private void addParameterName(MethodParameter param, Set<String> names) {
        // 优先使用 Java 形参名（编译时保留 -parameters 或 debug 信息时可用）
        String varName = param.getParameterName();
        if (varName != null) {
            names.add(varName);
            return;
        }
        // 形参名不可用时（极少数情况），展开类型字段名作为 fallback：
        // Springdoc 在将复杂对象作为 query 参数处理时可能以字段名展开。
        // 仅对排除类型做此展开，不影响其他参数。
        for (var field : param.getParameterType().getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                names.add(field.getName());
            }
        }
    }
}
