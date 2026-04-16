package ai.apiprism.protocol.registration;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * 中心处理注册请求后的结果。
 */
@Value
@Builder(toBuilder = true)
public class ApiRegistrationResponse {

    /**
     * 是否接受本次注册。
     */
    boolean accepted;

    /**
     * 注册记录的唯一标识。
     */
    String registrationId;

    /**
     * 给调用方的简要结果说明。
     */
    String message;

    /**
     * 规格归一化或存储阶段产生的非阻断告警。
     */
    @Singular(ignoreNullCollections = true)
    List<String> warnings;
}
