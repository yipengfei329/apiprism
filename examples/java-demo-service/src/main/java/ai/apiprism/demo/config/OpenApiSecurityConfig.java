package ai.apiprism.demo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

/**
 * 全局 OpenAPI 配置：声明 Demo 服务可用的鉴权方案。
 * <p>
 * 当前声明两种方案：
 * <ul>
 *   <li>{@code ApiKeyAuth}：基于 {@code X-Api-Key} 请求头的 API Key 鉴权，适用于 B 端开放平台/合作伙伴接口。</li>
 *   <li>{@code BearerAuth}：标准 OAuth2 Bearer Token，预留给后续场景使用。</li>
 * </ul>
 * 真实请求需在 Header 中携带对应的 API Key 才能访问受保护接口。
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "APIPrism Demo Service",
                version = "1.0",
                description = "覆盖复杂参数与鉴权场景的功能验证 Demo。"
        )
)
@SecuritySchemes({
        @SecurityScheme(
                name = "ApiKeyAuth",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-Api-Key",
                description = "开放平台 API Key。在合作伙伴控制台申请，请求头 X-Api-Key 携带。"
        ),
        @SecurityScheme(
                name = "BearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT",
                description = "标准 Bearer Token 鉴权，仅运营/管理后台接口使用。"
        )
})
public class OpenApiSecurityConfig {
}
