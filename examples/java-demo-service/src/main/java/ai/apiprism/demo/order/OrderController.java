package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 订单管理接口。
 * <p>
 * 覆盖测试场景：
 * <ul>
 *   <li>深层嵌套请求体（地址 + 行项目列表 + 支付信息）</li>
 *   <li>多维 query 参数（分页、枚举多选、日期区间、金额区间）</li>
 *   <li>路径参数 + 请求体组合（PATCH status）</li>
 *   <li>泛型分页响应体</li>
 * </ul>
 */
@RestController
@RequestMapping("/orders")
@Tag(name = "订单管理", description = "订单的创建、查询、状态流转接口")
public class OrderController {

    @Operation(
        summary = "创建订单",
        description = "提交包含收货地址、商品行、支付方式的完整订单。支持多优惠券叠加和自定义扩展字段。",
        responses = {
            @ApiResponse(responseCode = "201", description = "订单创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数校验失败"),
            @ApiResponse(responseCode = "422", description = "业务规则不满足，如库存不足")
        }
    )
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // Demo stub：返回固定数据
        OrderResponse resp = OrderResponse.builder()
                .orderId("ORD-20240101-0001")
                .status(OrderStatus.PENDING_PAYMENT)
                .userId(request.getUserId())
                .shippingAddress(request.getShippingAddress())
                .items(request.getItems())
                .subtotal(new BigDecimal("598.00"))
                .shippingFee(new BigDecimal("12.00"))
                .discountAmount(new BigDecimal("59.80"))
                .totalAmount(new BigDecimal("550.20"))
                .paymentMethod(request.getPaymentMethod())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return ResponseEntity.status(201).body(resp);
    }

    @Operation(
        summary = "查询订单列表",
        description = "支持多维过滤：状态多选、日期区间、金额区间、支付方式多选，以及分页排序。"
    )
    @GetMapping
    public ResponseEntity<PageResult<OrderResponse>> listOrders(@ParameterObject OrderQueryParams params) {
        OrderResponse stub = OrderResponse.builder()
                .orderId("ORD-20240101-0001")
                .status(OrderStatus.PAID)
                .userId("user-abc123")
                .subtotal(new BigDecimal("598.00"))
                .totalAmount(new BigDecimal("550.20"))
                .paymentMethod(PaymentMethod.ALIPAY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        PageResult<OrderResponse> result = PageResult.<OrderResponse>builder()
                .items(List.of(stub))
                .page(params.getPage())
                .size(params.getSize())
                .total(1L)
                .totalPages(1)
                .build();
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "查询订单详情",
        description = "根据订单号返回完整的订单信息，包括物流跟踪号和时间戳。",
        responses = {
            @ApiResponse(responseCode = "200", description = "订单信息"),
            @ApiResponse(responseCode = "404", description = "订单不存在",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @Parameter(description = "订单号", example = "ORD-20240101-0001")
            @PathVariable String orderId
    ) {
        OrderResponse resp = OrderResponse.builder()
                .orderId(orderId)
                .status(OrderStatus.SHIPPED)
                .userId("user-abc123")
                .subtotal(new BigDecimal("598.00"))
                .shippingFee(new BigDecimal("12.00"))
                .discountAmount(new BigDecimal("59.80"))
                .totalAmount(new BigDecimal("550.20"))
                .paymentMethod(PaymentMethod.WECHAT_PAY)
                .paidAt(Instant.now())
                .trackingNumber("SF1234567890")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "更新订单状态",
        description = "运营/客服手动推进订单状态，例如标记发货或处理取消请求。状态流转遵循业务规则约束。",
        responses = {
            @ApiResponse(responseCode = "200", description = "状态更新成功"),
            @ApiResponse(responseCode = "409", description = "当前状态不允许此次流转")
        }
    )
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @Parameter(description = "订单号", example = "ORD-20240101-0001")
            @PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse resp = OrderResponse.builder()
                .orderId(orderId)
                .status(request.getTargetStatus())
                .updatedAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "向订单追加商品行",
        description = "仅允许在 PENDING_PAYMENT 状态下追加商品，用于拆单前的购物车调整场景。"
    )
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse> addOrderItem(
            @Parameter(description = "订单号", example = "ORD-20240101-0001")
            @PathVariable String orderId,
            @RequestBody OrderItem item
    ) {
        OrderResponse resp = OrderResponse.builder()
                .orderId(orderId)
                .status(OrderStatus.PENDING_PAYMENT)
                .items(List.of(item))
                .updatedAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }
}
