package ai.apiprism.demo.notification;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 发送通知请求体。
 */
@Data
public class SendNotificationRequest {

    /** 目标用户 ID 列表，支持批量发送 */
    private List<String> targetUserIds;

    /** 通知类型 */
    private NotificationType type;

    /** 通知标题 */
    private String title;

    /** 通知正文内容 */
    private String content;

    /** 点击跳转链接 */
    private String actionUrl;

    /** 业务扩展数据 */
    private Map<String, String> extras;
}
