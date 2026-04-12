package ai.apiprism.adapter.starter.exceptions;

/**
 * 向 APIPrism center 发送注册请求失败时抛出的运行时异常。
 */
public class ApiPrismRegistrationException extends RuntimeException {

    public ApiPrismRegistrationException(String message) {
        super(message);
    }

    public ApiPrismRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
