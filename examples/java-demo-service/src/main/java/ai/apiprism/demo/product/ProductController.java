package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商品搜索与推荐接口。
 * <p>
 * 覆盖测试场景：
 * <ul>
 *   <li>POST body 包含 Map + List + 嵌套对象（GeoPoint）</li>
 *   <li>响应体含多层嵌套列表（FacetGroup → FacetBucket）</li>
 *   <li>query 参数对象（@ParameterObject 展开 RecommendContext）</li>
 *   <li>路径参数 + query 参数组合</li>
 * </ul>
 */
@RestController
@RequestMapping("/products")
@Tag(name = "商品搜索", description = "商品全文搜索、属性过滤和个性化推荐")
public class ProductController {

    @Operation(
        summary = "搜索商品",
        description = "结构化搜索接口，支持关键词、类目/品牌多选、价格区间、规格属性 Map 过滤、" +
                      "地理围栏以及 Facet 聚合维度请求。"
    )
    @PostMapping("/search")
    public ResponseEntity<ProductSearchResponse> searchProducts(
            @RequestBody ProductSearchRequest request
    ) {
        ProductSummary product = ProductSummary.builder()
                .productId("prod-001")
                .name("Sony WH-1000XM5 无线降噪耳机")
                .brand("Sony")
                .imageUrl("https://cdn.example.com/images/wh1000xm5.jpg")
                .originalPrice(new BigDecimal("2699.00"))
                .salePrice(new BigDecimal("2299.00"))
                .rating(4.8)
                .reviewCount(12800)
                .monthlySales(3500)
                .inStock(true)
                .matchedAttributes(Map.of("color", "星空黑", "connectivity", "蓝牙5.3"))
                .tags(List.of("热销", "官方直营"))
                .build();

        FacetGroup brandFacet = FacetGroup.builder()
                .dimension("brand")
                .displayName("品牌")
                .buckets(List.of(
                    FacetBucket.builder().key("brand-sony").label("Sony").count(42L).selected(false).build(),
                    FacetBucket.builder().key("brand-apple").label("Apple").count(38L).selected(false).build()
                ))
                .build();

        ProductSearchResponse resp = ProductSearchResponse.builder()
                .products(List.of(product))
                .total(256L)
                .page(request.getPage())
                .size(request.getSize())
                .tookMs(42L)
                .cached(false)
                .facets(request.getFacets() != null ? List.of(brandFacet) : null)
                .build();

        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "获取商品详情页推荐",
        description = "根据商品 ID 和推荐上下文（用户、策略、场景标识）返回个性化推荐列表，" +
                      "用于商品详情页侧栏/底部猜你喜欢区域。"
    )
    @GetMapping("/{productId}/recommendations")
    public ResponseEntity<List<ProductSummary>> getRecommendations(
            @Parameter(description = "商品 ID", example = "prod-001")
            @PathVariable String productId,
            @ParameterObject RecommendContext context
    ) {
        ProductSummary rec = ProductSummary.builder()
                .productId("prod-002")
                .name("Bose QuietComfort 45 无线耳机")
                .brand("Bose")
                .originalPrice(new BigDecimal("2499.00"))
                .salePrice(new BigDecimal("1999.00"))
                .rating(4.7)
                .reviewCount(8900)
                .monthlySales(2100)
                .inStock(true)
                .tags(List.of("同类热销"))
                .build();

        return ResponseEntity.ok(List.of(rec));
    }

    @Operation(
        summary = "批量查询商品库存与价格快照",
        description = "给定 SKU ID 列表，批量返回实时库存数量和当前售价。适用于购物车、对比页等需要实时刷新的场景。"
    )
    @PostMapping("/snapshot")
    public ResponseEntity<List<SkuSnapshot>> getSkuSnapshots(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "SKU ID 列表，最多 50 条"
            )
            @RequestBody List<String> skuIds
    ) {
        List<SkuSnapshot> snapshots = skuIds.stream()
                .map(id -> SkuSnapshot.builder()
                        .skuId(id)
                        .currentPrice(new BigDecimal("299.00"))
                        .stockQuantity(100)
                        .available(true)
                        .build())
                .toList();
        return ResponseEntity.ok(snapshots);
    }
}
