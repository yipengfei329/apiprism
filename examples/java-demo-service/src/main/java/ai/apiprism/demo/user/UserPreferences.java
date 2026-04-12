package ai.apiprism.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 用户偏好设置。 */
@Data
@Schema(description = "用户偏好设置")
public class UserPreferences {

    @Schema(description = "界面语言", example = "zh-CN",
            allowableValues = {"zh-CN", "zh-TW", "en-US", "ja-JP"})
    private String language;

    @Schema(description = "时区", example = "Asia/Shanghai")
    private String timezone;

    @Schema(description = "货币单位", example = "CNY",
            allowableValues = {"CNY", "USD", "EUR", "JPY"})
    private String currency;

    @Schema(description = "启用的通知渠道列表")
    private List<NotificationChannel> notificationChannels;

    @Schema(description = "各类事件的通知开关（事件名 -> 是否启用）",
            example = "{\"order_paid\": true, \"order_shipped\": true, \"promotion\": false}")
    private Map<String, Boolean> notificationSubscriptions;

    @Schema(description = "感兴趣的商品类目 ID 列表", example = "[\"cat-001\", \"cat-007\"]")
    private List<String> interestedCategories;

    @Schema(description = "是否启用无障碍模式", example = "false")
    private Boolean accessibilityMode;
}
