package ai.apiprism.adapter.starter.exceptions;

/**
 * 向 APIPrism center 发送注册请求失败时抛出的运行时异常。
 * <p>
 * {@link #retryable} 标记用于区分可重试的瞬态故障（如 5xx、网络超时）
 * 和不可重试的客户端错误（如 4xx），供重试框架决策是否继续重试。
 */
public class ApiPrismRegistrationException extends RuntimeException {

    private final boolean retryable;

    public ApiPrismRegistrationException(String message) {
        this(message, true);
    }

    public ApiPrismRegistrationException(String message, Throwable cause) {
        this(message, cause, true);
    }

    public ApiPrismRegistrationException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public ApiPrismRegistrationException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
