package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品搜索请求体。
 * <p>
 * 故意设计为 POST /products/search（而非 GET），以便携带复杂结构化过滤条件。
 * 覆盖：自由文本搜索、多级类目过滤、价格区间、属性键值过滤（Map）、地理围栏、Facet 聚合请求。
 */
@Data
@Schema(description = "商品搜索请求")
public class ProductSearchRequest {

    @Schema(description = "搜索关键词（支持空格分隔的多词）", example = "无线 蓝牙 耳机")
    private String keyword;

    @Schema(description = "类目 ID 列表（OR 关系，匹配任一类目即返回）",
            example = "[\"cat-001\", \"cat-007\"]")
    private List<String> categoryIds;

    @Schema(description = "品牌 ID 列表（OR 关系）", example = "[\"brand-apple\", \"brand-sony\"]")
    private List<String> brandIds;

    @Schema(description = "最低价格（元，含）", example = "100.00")
    private BigDecimal minPrice;

    @Schema(description = "最高价格（元，含）", example = "999.00")
    private BigDecimal maxPrice;

    @Schema(
        description = "规格属性过滤（AND 关系）。key 为属性名，value 为允许的属性值列表（OR 关系）。" +
                      "例如：{\"color\": [\"黑色\", \"白色\"], \"storage\": [\"256G\"]}",
        example = "{\"color\": [\"黑色\"], \"connectivity\": [\"蓝牙5.3\"]}"
    )
    private Map<String, List<String>> attributes;

    @Schema(description = "地理位置过滤（附近商品/门店）")
    private GeoPoint location;

    @Schema(description = "是否仅显示有货商品", example = "true")
    private Boolean inStockOnly;

    @Schema(description = "评分下限（1.0-5.0）", example = "4.0")
    private Double minRating;

    @Schema(description = "排序方式",
            allowableValues = {"relevance", "price_asc", "price_desc", "sales_desc", "rating_desc", "newest"},
            example = "relevance")
    private String sortBy;

    @Schema(description = "需要聚合返回的 Facet 维度列表",
            example = "[\"category\", \"brand\", \"price_range\", \"color\"]")
    private List<String> facets;

    @Schema(description = "页码，从 0 开始（对齐搜索引擎惯例）", example = "0")
    private int page = 0;

    @Schema(description = "每页条数", example = "24")
    private int size = 24;
}
