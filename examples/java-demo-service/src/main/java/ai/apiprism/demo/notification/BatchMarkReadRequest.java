package ai.apiprism.demo.notification;

import lombok.Data;

import java.util.List;

/**
 * 批量标记已读请求体。
 */
@Data
public class BatchMarkReadRequest {

    /** 通知 ID 列表，最多 100 条 */
    private List<String> notificationIds;
}
