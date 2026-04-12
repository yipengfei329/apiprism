package ai.apiprism.demo.notification;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 未读通知计数响应体。
 */
@Data
@Builder
public class UnreadCountResponse {

    /** 未读总数 */
    private long total;

    /** 按类型分组的未读数 */
    private Map<NotificationType, Long> countByType;
}
