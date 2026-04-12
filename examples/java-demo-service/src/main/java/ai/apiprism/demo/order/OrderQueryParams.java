package ai.apiprism.demo.order;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单列表查询参数（通过 @ParameterObject 展开为独立 query 参数）。
 * 覆盖：分页、枚举过滤、日期范围、金额区间、多值列表。
 */
@Data
public class OrderQueryParams {

    @Parameter(description = "页码，从 1 开始", example = "1")
    private int page = 1;

    @Parameter(description = "每页条数，最大 100", example = "20")
    private int size = 20;

    @Parameter(description = "排序字段", example = "createdAt",
               schema = @io.swagger.v3.oas.annotations.media.Schema(
                   allowableValues = {"createdAt", "totalAmount", "status"}))
    private String sortBy = "createdAt";

    @Parameter(description = "排序方向", example = "DESC",
               schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"ASC", "DESC"}))
    private String sortDir = "DESC";

    @Parameter(description = "按订单状态过滤（可多选）")
    private List<OrderStatus> statuses;

    @Parameter(description = "下单用户 ID", example = "user-abc123")
    private String userId;

    @Parameter(description = "下单起始日期（含）", example = "2024-01-01")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @Parameter(description = "下单结束日期（含）", example = "2024-12-31")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    @Parameter(description = "最小实付金额（元）", example = "100.00")
    private BigDecimal minAmount;

    @Parameter(description = "最大实付金额（元）", example = "500.00")
    private BigDecimal maxAmount;

    @Parameter(description = "支付方式过滤")
    private List<PaymentMethod> paymentMethods;
}
