package ai.apiprism.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * 中心内部定位一个服务快照的最小身份信息。
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class ServiceRef {

    /**
     * 服务稳定名称。
     */
    @NonNull
    String name;

    /**
     * 服务所属环境。
     */
    @NonNull
    String environment;
}
