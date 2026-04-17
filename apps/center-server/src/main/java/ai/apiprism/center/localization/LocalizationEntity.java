package ai.apiprism.center.localization;

/**
 * 可本地化实体类型枚举，值写入 content_localizations.entity_type。
 */
public enum LocalizationEntity {

    SERVICE("service"),
    TAG("tag"),
    OPERATION("operation"),
    OPERATION_PARAMETER("operation_parameter"),
    OPERATION_REQUEST_BODY("operation_request_body"),
    OPERATION_RESPONSE("operation_response");

    private final String code;

    LocalizationEntity(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
