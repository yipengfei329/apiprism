package ai.apiprism.demo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 推荐上下文（控制推荐策略的 query 参数组）。 */
@Data
@Schema(description = "推荐上下文参数")
public class RecommendContext {

    @Schema(description = "请求推荐的用户 ID（匿名时可不传）", example = "user-abc123")
    private String userId;

    @Schema(description = "推荐策略", example = "collaborative",
            allowableValues = {"collaborative", "content_based", "trending", "similar_items"})
    private String strategy;

    @Schema(description = "返回数量上限", example = "10")
    private int limit = 10;

    @Schema(description = "是否过滤掉用户已购买的商品", example = "true")
    private boolean excludePurchased = true;

    @Schema(description = "业务场景标识，用于 A/B 实验分流", example = "pdp_sidebar")
    private String scene;
}
