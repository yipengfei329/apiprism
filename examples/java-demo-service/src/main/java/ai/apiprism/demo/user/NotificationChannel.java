package ai.apiprism.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;

/** 通知渠道枚举。 */
@Schema(description = "通知渠道")
public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    IN_APP
}
