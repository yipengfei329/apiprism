package ai.apiprism.adapter.starter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记某个参数或自定义注解应当从 APIPrism 的 API 文档中隐藏。
 *
 * <p>典型用法：标注在自定义的 Session/Auth 注解上（元注解），使所有使用该注解的
 * 控制器参数自动从文档中过滤，无需在每个接口上单独处理。
 *
 * <pre>{@code
 * // 在自定义 Session 注解上加一次即可
 * @ApiHidden
 * @Target(ElementType.PARAMETER)
 * @Retention(RetentionPolicy.RUNTIME)
 * public @interface SessionUser {}
 *
 * // 之后所有使用 @SessionUser 的参数都不会出现在文档中
 * @PostMapping("/run")
 * public Result run(@SessionUser UserInfo user, @RequestBody RunRequest req) { ... }
 * }</pre>
 *
 * <p>也可以直接标注在控制器的参数上：
 * <pre>{@code
 * public Result run(@ApiHidden UserInfo user, @RequestBody RunRequest req) { ... }
 * }</pre>
 *
 * <p>对于无法通过注解方式处理的场景（第三方类型），可通过配置项
 * {@code apiprism.excluded-parameter-types} 指定类型全限定名列表实现相同效果。
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiHidden {
}
