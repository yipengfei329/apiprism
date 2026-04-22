package ai.apiprism.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * OAuth2 单个授权流程定义。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class CanonicalOAuthFlow {

    /** 流程类型：implicit、password、clientCredentials、authorizationCode */
    String flowType;

    /** 授权端点 URL（implicit / authorizationCode 流程必填） */
    String authorizationUrl;

    /** Token 端点 URL（password / clientCredentials / authorizationCode 流程必填） */
    String tokenUrl;

    /** Token 刷新端点 URL（可选） */
    String refreshUrl;

    /** scope 名称 → 描述映射 */
    @Singular(value = "scope", ignoreNullCollections = true)
    Map<String, String> scopes;
}
