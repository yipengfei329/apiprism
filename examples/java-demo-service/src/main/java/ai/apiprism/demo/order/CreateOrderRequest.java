package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/** 创建订单请求体，包含多层嵌套结构。 */
@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @Schema(description = "下单用户 ID", example = "user-abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userId;

    @Schema(description = "订单来源渠道", example = "APP", allowableValues = {"APP", "WEB", "MINI_PROGRAM"})
    private String channel;

    @Schema(description = "收货地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private Address shippingAddress;

    @Schema(description = "账单地址（不填则与收货地址相同）")
    private Address billingAddress;

    @Schema(description = "订单行列表，至少包含一项", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<OrderItem> items;

    @Schema(description = "支付方式", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethod paymentMethod;

    @Schema(description = "优惠券代码列表，最多同时使用 3 张", example = "[\"COUPON-2024\"]")
    private List<String> couponCodes;

    @Schema(description = "用户备注", example = "请在工作日配送")
    private String customerNote;

    @Schema(
        description = "自定义扩展属性（key-value 键值对，用于对接三方系统）",
        example = "{\"erp_order_id\": \"ERP-20240101-999\"}"
    )
    private Map<String, String> extensions;
}
