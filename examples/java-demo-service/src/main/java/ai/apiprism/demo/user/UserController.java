package ai.apiprism.demo.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 用户账户接口。
 * <p>
 * 覆盖测试场景：
 * <ul>
 *   <li>枚举字段（gender / memberLevel / status）</li>
 *   <li>Map 类型字段（notificationSubscriptions）</li>
 *   <li>深层嵌套（UserPreferences + AccountSummary 内嵌）</li>
 *   <li>PUT 全量替换偏好与 PATCH 局部更新对比</li>
 * </ul>
 */
@RestController
@RequestMapping("/users")
@Tag(name = "用户账户", description = "用户注册、查询和偏好设置管理")
public class UserController {

    @Operation(
        summary = "注册新用户",
        description = "创建用户账号，支持在注册时同步设置偏好，并可绑定邀请人关系。",
        responses = {
            @ApiResponse(responseCode = "201", description = "注册成功"),
            @ApiResponse(responseCode = "409", description = "用户名或邮箱已存在",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        UserResponse resp = buildStubUser("user-abc123", request.getUsername(), request.getEmail());
        return ResponseEntity.status(201).body(resp);
    }

    @Operation(
        summary = "查询用户详情",
        description = "返回用户完整信息，包含偏好设置和账户资产摘要。",
        responses = {
            @ApiResponse(responseCode = "200", description = "用户信息"),
            @ApiResponse(responseCode = "404", description = "用户不存在",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "用户 ID", example = "user-abc123")
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(buildStubUser(userId, "zhangsan", "zhangsan@example.com"));
    }

    @Operation(
        summary = "全量替换用户偏好设置",
        description = "使用 PUT 语义替换全部偏好字段，未传字段将被重置为默认值。"
    )
    @PutMapping("/{userId}/preferences")
    public ResponseEntity<UserResponse> replacePreferences(
            @Parameter(description = "用户 ID", example = "user-abc123")
            @PathVariable String userId,
            @RequestBody UserPreferences preferences
    ) {
        UserResponse resp = buildStubUser(userId, "zhangsan", "zhangsan@example.com");
        resp.setPreferences(preferences);
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "局部更新用户偏好设置",
        description = "使用 PATCH 语义仅更新传入的字段，未传字段保持不变。适合移动端按需更新场景。"
    )
    @PatchMapping("/{userId}/preferences")
    public ResponseEntity<UserResponse> patchPreferences(
            @Parameter(description = "用户 ID", example = "user-abc123")
            @PathVariable String userId,
            @RequestBody UserPreferences patch
    ) {
        UserResponse resp = buildStubUser(userId, "zhangsan", "zhangsan@example.com");
        resp.setPreferences(patch);
        return ResponseEntity.ok(resp);
    }

    // ---- 私有工具方法 ----

    private UserResponse buildStubUser(String userId, String username, String email) {
        UserPreferences prefs = new UserPreferences();
        prefs.setLanguage("zh-CN");
        prefs.setTimezone("Asia/Shanghai");
        prefs.setCurrency("CNY");
        prefs.setNotificationChannels(List.of(NotificationChannel.EMAIL, NotificationChannel.PUSH));

        AccountSummary summary = AccountSummary.builder()
                .balance(new BigDecimal("128.50"))
                .points(2500L)
                .couponCount(3)
                .memberLevel("GOLD")
                .totalSpent(new BigDecimal("12800.00"))
                .build();

        return UserResponse.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .phone("+8613800138000")
                .realName("张三")
                .birthday(LocalDate.of(1990, 6, 15))
                .status("ACTIVE")
                .preferences(prefs)
                .accountSummary(summary)
                .createdAt(Instant.now())
                .lastLoginAt(Instant.now())
                .build();
    }
}
