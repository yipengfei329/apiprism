package ai.apiprism.demo.notification;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * 通知详情响应体。
 */
@Data
@Builder
public class NotificationResponse {

    /** 通知唯一标识 */
    private String notificationId;

    /** 通知类型 */
    private NotificationType type;

    /** 通知标题 */
    private String title;

    /** 通知正文内容，支持富文本 */
    private String content;

    /** 当前状态 */
    private NotificationStatus status;

    /** 发送方标识，系统通知为 "system" */
    private String sender;

    /** 点击通知后的跳转链接 */
    private String actionUrl;

    /** 业务扩展数据，如订单号、活动 ID 等 */
    private Map<String, String> extras;

    /** 创建时间 */
    private Instant createdAt;

    /** 已读时间，未读时为 null */
    private Instant readAt;
}
