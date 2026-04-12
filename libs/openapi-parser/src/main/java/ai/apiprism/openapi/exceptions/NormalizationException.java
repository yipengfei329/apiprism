package ai.apiprism.openapi.exceptions;

/**
 * OpenAPI 规范解析或规范化失败时抛出的运行时异常。
 */
public class NormalizationException extends RuntimeException {

    public NormalizationException(String message) {
        super(message);
    }
}
