package ai.apiprism.demo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/** 注册用户请求体。 */
@Data
@Schema(description = "注册用户请求")
public class CreateUserRequest {

    @Schema(description = "用户名（唯一）", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "邮箱", example = "zhangsan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "手机号（E.164 格式）", example = "+8613800138000")
    private String phone;

    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Schema(description = "出生日期", example = "1990-06-15")
    private LocalDate birthday;

    @Schema(description = "性别", allowableValues = {"MALE", "FEMALE", "OTHER", "PREFER_NOT_TO_SAY"})
    private String gender;

    @Schema(description = "初始偏好设置，可在注册时一并配置")
    private UserPreferences preferences;

    @Schema(description = "邀请码（推荐人注册场景）", example = "INV-XYZ789")
    private String inviteCode;
}
