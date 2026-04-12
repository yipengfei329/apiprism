package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** 订单详情响应体。 */
@Data
@Builder
@Schema(description = "订单详情")
public class OrderResponse {

    @Schema(description = "订单号", example = "ORD-20240101-0001")
    private String orderId;

    @Schema(description = "订单状态")
    private OrderStatus status;

    @Schema(description = "下单用户 ID", example = "user-abc123")
    private String userId;

    @Schema(description = "收货地址")
    private Address shippingAddress;

    @Schema(description = "订单行列表")
    private List<OrderItem> items;

    @Schema(description = "商品总金额（元）", example = "598.00")
    private BigDecimal subtotal;

    @Schema(description = "运费（元）", example = "12.00")
    private BigDecimal shippingFee;

    @Schema(description = "优惠金额（元）", example = "59.80")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额（元）", example = "550.20")
    private BigDecimal totalAmount;

    @Schema(description = "支付方式")
    private PaymentMethod paymentMethod;

    @Schema(description = "支付时间（ISO-8601）", example = "2024-01-01T10:00:00Z")
    private Instant paidAt;

    @Schema(description = "物流单号", example = "SF1234567890")
    private String trackingNumber;

    @Schema(description = "创建时间（ISO-8601）", example = "2024-01-01T09:00:00Z")
    private Instant createdAt;

    @Schema(description = "最后更新时间（ISO-8601）", example = "2024-01-01T10:00:00Z")
    private Instant updatedAt;
}
