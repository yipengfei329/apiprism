package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 商品搜索响应（含 Facet 聚合）。 */
@Data
@Builder
@Schema(description = "商品搜索响应")
public class ProductSearchResponse {

    @Schema(description = "搜索命中的商品列表")
    private List<ProductSummary> products;

    @Schema(description = "总命中数", example = "256")
    private long total;

    @Schema(description = "当前页码", example = "0")
    private int page;

    @Schema(description = "每页条数", example = "24")
    private int size;

    @Schema(description = "查询耗时（毫秒）", example = "42")
    private long tookMs;

    @Schema(description = "是否命中缓存", example = "false")
    private boolean cached;

    @Schema(description = "Facet 聚合结果列表（仅当请求中 facets 非空时返回）")
    private List<FacetGroup> facets;

    @Schema(description = "搜索引擎纠错建议词（当关键词疑似拼写错误时）", example = "蓝牙耳机")
    private String didYouMean;
}
