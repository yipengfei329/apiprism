package ai.apiprism.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/** 用户详情响应体（含嵌套偏好和账户摘要）。 */
@Data
@Builder
@Schema(description = "用户详情")
public class UserResponse {

    @Schema(description = "用户 ID", example = "user-abc123")
    private String userId;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号", example = "+8613800138000")
    private String phone;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "出生日期", example = "1990-06-15")
    private LocalDate birthday;

    @Schema(description = "账户状态", allowableValues = {"ACTIVE", "LOCKED", "DEACTIVATED"})
    private String status;

    @Schema(description = "偏好设置")
    private UserPreferences preferences;

    @Schema(description = "账户资产摘要")
    private AccountSummary accountSummary;

    @Schema(description = "注册时间（ISO-8601）", example = "2024-01-01T09:00:00Z")
    private Instant createdAt;

    @Schema(description = "最后登录时间（ISO-8601）", example = "2024-03-15T08:30:00Z")
    private Instant lastLoginAt;
}
