package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.media.Schema;

/** 税费类型。 */
@Schema(description = "税费类型")
public enum TaxType {
    @Schema(description = "增值税")
    VAT,
    @Schema(description = "商品及服务税")
    GST,
    @Schema(description = "销售税")
    SALES_TAX,
    @Schema(description = "代扣代缴税")
    WITHHOLDING_TAX
}
