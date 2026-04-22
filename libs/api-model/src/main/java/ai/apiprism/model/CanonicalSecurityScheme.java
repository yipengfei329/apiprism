package ai.apiprism.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

/**
 * 归一化后的安全方案定义（来自 OpenAPI components.securitySchemes）。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class CanonicalSecurityScheme {

    /** 方案类型：http / apiKey / oauth2 / openIdConnect / mutualTLS */
    String type;

    /** HTTP 子类型：bearer / basic（type=http 时有效） */
    String scheme;

    /** Bearer Token 格式说明，如 JWT（type=http, scheme=bearer 时可选） */
    String bearerFormat;

    /** API Key 传递位置：header / query / cookie（type=apiKey 时有效） */
    String in;

    /** API Key 参数名，如 X-Api-Key（type=apiKey 时有效） */
    String paramName;

    /** OpenID Connect 发现端点 URL（type=openIdConnect 时有效） */
    String openIdConnectUrl;

    /** 人类可读说明 */
    String description;

    /** OAuth2 授权流程列表（type=oauth2 时有效） */
    @Singular(value = "oauthFlow", ignoreNullCollections = true)
    List<CanonicalOAuthFlow> oauthFlows;
}
