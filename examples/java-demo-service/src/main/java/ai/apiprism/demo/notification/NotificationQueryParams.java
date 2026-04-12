package ai.apiprism.demo.notification;

import lombok.Data;

import java.util.List;

/**
 * 通知列表查询参数。
 */
@Data
public class NotificationQueryParams {

    /** 按通知类型过滤，支持多选 */
    private List<NotificationType> types;

    /** 按状态过滤 */
    private NotificationStatus status;

    /** 页码，从 1 开始 */
    private int page = 1;

    /** 每页条数，默认 20 */
    private int size = 20;
}
