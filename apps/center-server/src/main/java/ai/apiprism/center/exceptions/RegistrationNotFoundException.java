package ai.apiprism.center.exceptions;

/**
 * 按服务名和环境查找注册记录时未找到对应条目时抛出的异常。
 */
public class RegistrationNotFoundException extends RuntimeException {

    public RegistrationNotFoundException(String serviceName, String environment) {
        super("Service not found: " + serviceName + " (" + environment + ")");
    }
}
