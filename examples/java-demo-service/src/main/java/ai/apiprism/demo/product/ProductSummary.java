package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 搜索结果中的商品摘要。 */
@Data
@Builder
@Schema(description = "商品摘要")
public class ProductSummary {

    @Schema(description = "商品 ID", example = "prod-001")
    private String productId;

    @Schema(description = "商品名称", example = "Sony WH-1000XM5 无线降噪耳机")
    private String name;

    @Schema(description = "品牌", example = "Sony")
    private String brand;

    @Schema(description = "主图 URL", example = "https://cdn.example.com/images/wh1000xm5.jpg")
    private String imageUrl;

    @Schema(description = "原价（元）", example = "2699.00")
    private BigDecimal originalPrice;

    @Schema(description = "促销价（元），null 表示无促销", example = "2299.00")
    private BigDecimal salePrice;

    @Schema(description = "综合评分（1.0-5.0）", example = "4.8")
    private Double rating;

    @Schema(description = "评价数量", example = "12800")
    private Integer reviewCount;

    @Schema(description = "月销量", example = "3500")
    private Integer monthlySales;

    @Schema(description = "是否有货", example = "true")
    private Boolean inStock;

    @Schema(description = "命中的规格属性快照", example = "{\"color\": \"星空黑\", \"connectivity\": \"蓝牙5.3\"}")
    private Map<String, String> matchedAttributes;

    @Schema(description = "标签列表（新品/热销/官方直营）", example = "[\"热销\", \"官方直营\"]")
    private List<String> tags;
}
