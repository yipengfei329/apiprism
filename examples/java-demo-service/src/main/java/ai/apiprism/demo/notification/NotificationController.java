package ai.apiprism.demo.notification;

import ai.apiprism.demo.order.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 消息通知接口。
 * <p>
 * 本控制器演示纯 Javadoc 文档注释风格：所有接口描述、参数说明、返回值说明
 * 均通过 Javadoc 而非 {@code @Operation} / {@code @Schema} 注解实现。
 * 依赖 {@code springdoc-openapi-javadoc} 在编译期自动提取注释并生成 OpenAPI 文档。
 * <p>
 * 覆盖测试场景：
 * <ul>
 *   <li>纯 Javadoc 驱动的 OpenAPI 文档生成</li>
 *   <li>路径参数与 query 参数组合</li>
 *   <li>批量操作请求体</li>
 *   <li>聚合统计响应</li>
 * </ul>
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "消息通知", description = "通知的发送、查询、已读标记和统计接口（Javadoc 文档风格示范）")
public class NotificationController {

    /**
     * 发送通知。
     * <p>
     * 向指定用户批量发送通知消息，支持系统公告、订单变更、营销推送等多种类型。
     * 单次请求最多指定 1000 个目标用户。
     *
     * @param request 发送通知请求体
     * @return 发送成功的通知列表
     */
    @PostMapping
    public ResponseEntity<List<NotificationResponse>> sendNotification(
            @RequestBody SendNotificationRequest request
    ) {
        // Demo stub
        List<NotificationResponse> results = request.getTargetUserIds().stream()
                .map(userId -> NotificationResponse.builder()
                        .notificationId("ntf-" + userId + "-001")
                        .type(request.getType())
                        .title(request.getTitle())
                        .content(request.getContent())
                        .status(NotificationStatus.UNREAD)
                        .sender("system")
                        .actionUrl(request.getActionUrl())
                        .extras(request.getExtras())
                        .createdAt(Instant.now())
                        .build())
                .toList();
        return ResponseEntity.status(201).body(results);
    }

    /**
     * 查询用户通知列表。
     * <p>
     * 支持按通知类型、状态过滤，返回分页结果。默认按创建时间倒序排列。
     *
     * @param userId 用户 ID
     * @param params 查询参数：类型过滤、状态过滤、分页
     * @return 分页通知列表
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<PageResult<NotificationResponse>> listNotifications(
            @PathVariable String userId,
            @ParameterObject NotificationQueryParams params
    ) {
        NotificationResponse stub = NotificationResponse.builder()
                .notificationId("ntf-001")
                .type(NotificationType.ORDER)
                .title("您的订单已发货")
                .content("订单 ORD-20240101-0001 已由顺丰快递揽收，运单号 SF1234567890。")
                .status(NotificationStatus.UNREAD)
                .sender("system")
                .actionUrl("/orders/ORD-20240101-0001")
                .extras(Map.of("orderId", "ORD-20240101-0001"))
                .createdAt(Instant.now())
                .build();

        PageResult<NotificationResponse> result = PageResult.<NotificationResponse>builder()
                .items(List.of(stub))
                .page(params.getPage())
                .size(params.getSize())
                .total(1L)
                .totalPages(1)
                .build();
        return ResponseEntity.ok(result);
    }

    /**
     * 查询单条通知详情。
     *
     * @param notificationId 通知唯一标识
     * @return 通知详情，不存在时返回 404
     */
    @GetMapping("/{notificationId}")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable String notificationId
    ) {
        NotificationResponse resp = NotificationResponse.builder()
                .notificationId(notificationId)
                .type(NotificationType.PROMOTION)
                .title("618 大促开启")
                .content("您有 3 张满减券即将到期，快去使用吧！")
                .status(NotificationStatus.UNREAD)
                .sender("system")
                .actionUrl("/promotions/618")
                .extras(Map.of("campaignId", "618-2024"))
                .createdAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * 标记单条通知为已读。
     *
     * @param notificationId 通知唯一标识
     * @return 更新后的通知详情
     */
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable String notificationId
    ) {
        NotificationResponse resp = NotificationResponse.builder()
                .notificationId(notificationId)
                .type(NotificationType.ORDER)
                .title("您的订单已发货")
                .status(NotificationStatus.READ)
                .sender("system")
                .createdAt(Instant.now())
                .readAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    /**
     * 批量标记通知为已读。
     * <p>
     * 一次性将多条通知标记为已读状态，适用于"全部已读"或勾选批量操作场景。
     * 单次请求最多处理 100 条。
     *
     * @param userId  用户 ID
     * @param request 包含待标记通知 ID 列表的请求体
     * @return 204 无内容
     */
    @PatchMapping("/users/{userId}/read")
    public ResponseEntity<Void> batchMarkAsRead(
            @PathVariable String userId,
            @RequestBody BatchMarkReadRequest request
    ) {
        return ResponseEntity.noContent().build();
    }

    /**
     * 查询用户未读通知数量。
     * <p>
     * 返回未读总数及按通知类型分组的计数，适用于导航栏角标和通知中心入口展示。
     *
     * @param userId 用户 ID
     * @return 未读计数统计
     */
    @GetMapping("/users/{userId}/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @PathVariable String userId
    ) {
        UnreadCountResponse resp = UnreadCountResponse.builder()
                .total(5L)
                .countByType(Map.of(
                        NotificationType.ORDER, 2L,
                        NotificationType.PROMOTION, 2L,
                        NotificationType.SOCIAL, 1L
                ))
                .build();
        return ResponseEntity.ok(resp);
    }
}
