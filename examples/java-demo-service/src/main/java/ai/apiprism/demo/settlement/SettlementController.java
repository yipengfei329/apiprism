package ai.apiprism.demo.settlement;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * 商户结算 / 出款开放接口。
 * <p>
 * 所有接口要求请求头 {@code X-Api-Key} 携带商户开放平台 API Key。
 * <p>
 * 覆盖测试场景：
 * <ul>
 *   <li>四层嵌套请求体（结算行 → 费用 / 税费 / 汇兑 → Money）</li>
 *   <li>响应包含 List 嵌套 List（CurrencyAccount → SubLedger）</li>
 *   <li>组合鉴权：API Key（默认） + Bearer（敏感操作 cancel）</li>
 *   <li>Header 参数 X-Idempotency-Key 与签名 X-Signature</li>
 * </ul>
 */
@RestController
@RequestMapping("/settlements")
@Tag(name = "结算与出款", description = "商户批量结算、出款追踪和余额快照接口（需要 API Key）")
@SecurityRequirement(name = "ApiKeyAuth")
public class SettlementController {

    @Operation(
        summary = "提交批量结算",
        description = "将一段账期内的多笔订单/退款打包结算并触发出款。" +
                      "推荐由商户系统通过 cron 或对账驱动；调用方需用 X-Idempotency-Key 保证幂等。",
        responses = {
            @ApiResponse(responseCode = "202", description = "已受理，进入审批/出款流程"),
            @ApiResponse(responseCode = "400", description = "结算行金额对不齐或缺失合规字段"),
            @ApiResponse(responseCode = "401", description = "API Key 缺失或无效",
                         content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "批次号重复（幂等命中将返回 200 而非 409）",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @PostMapping("/batches")
    public ResponseEntity<BatchSettlementResponse> submitBatch(
            @Parameter(description = "幂等键（建议 UUID）", example = "idem-batch-2024-04-25-001")
            @RequestHeader(value = "X-Idempotency-Key") String idempotencyKey,
            @Parameter(description = "请求体 HMAC-SHA256 签名（hex），密钥见控制台",
                       example = "9f1e6c8b...4d2a")
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody BatchSettlementRequest request
    ) {
        Money totalGross = new Money(new BigDecimal("12999.00"), "CNY");
        Money totalFees = new Money(new BigDecimal("78.50"), "CNY");
        Money totalNet = new Money(new BigDecimal("12920.50"), "CNY");

        SettlementLineResult lineResult = SettlementLineResult.builder()
                .orderRef("ORD-20240425-9001")
                .status("SUCCESS")
                .settledAmount(new Money(new BigDecimal("1290.50"), "CNY"))
                .build();

        PayoutTracking tracking = PayoutTracking.builder()
                .payoutId("payout-2024-04-25-9001")
                .status("IN_FLIGHT")
                .events(List.of(
                        PayoutEvent.builder()
                                .occurredAt(Instant.now().minusSeconds(120))
                                .code("SUBMITTED")
                                .description("已提交至招商银行通道")
                                .externalReference("CMB-202404251638-0001")
                                .build(),
                        PayoutEvent.builder()
                                .occurredAt(Instant.now())
                                .code("ACCEPTED_BY_BANK")
                                .description("招商银行已受理")
                                .externalReference("CMB-202404251642-9999")
                                .build()
                ))
                .build();

        BatchSettlementResponse resp = BatchSettlementResponse.builder()
                .batchId(request.getBatchId())
                .settlementId("settle-2024-04-25-9001")
                .status(SettlementStatus.PROCESSING)
                .totalGross(totalGross)
                .totalFees(totalFees)
                .totalNet(totalNet)
                .lineResults(List.of(lineResult))
                .payoutTracking(tracking)
                .submittedAt(Instant.now())
                .completedAt(null)
                .build();
        return ResponseEntity.status(202).body(resp);
    }

    @Operation(
        summary = "查询结算批次状态",
        description = "通过结算 ID 查询当前批次的状态、行级处理结果与出款追踪。"
    )
    @GetMapping("/batches/{settlementId}")
    public ResponseEntity<BatchSettlementResponse> getBatch(
            @Parameter(description = "系统结算 ID", example = "settle-2024-04-25-9001")
            @PathVariable String settlementId
    ) {
        BatchSettlementResponse resp = BatchSettlementResponse.builder()
                .batchId("batch-2024-04-25-001")
                .settlementId(settlementId)
                .status(SettlementStatus.SUCCEEDED)
                .totalGross(new Money(new BigDecimal("12999.00"), "CNY"))
                .totalFees(new Money(new BigDecimal("78.50"), "CNY"))
                .totalNet(new Money(new BigDecimal("12920.50"), "CNY"))
                .lineResults(List.of(SettlementLineResult.builder()
                        .orderRef("ORD-20240425-9001")
                        .status("SUCCESS")
                        .settledAmount(new Money(new BigDecimal("1290.50"), "CNY"))
                        .build()))
                .payoutTracking(PayoutTracking.builder()
                        .payoutId("payout-2024-04-25-9001")
                        .status("POSTED")
                        .events(List.of())
                        .build())
                .submittedAt(Instant.now().minusSeconds(3600))
                .completedAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "取消结算批次",
        description = "仅在 PENDING_APPROVAL 状态可取消。该接口属于资金敏感操作，可以使用 " +
                      "X-Api-Key 或 Bearer Token 中的任一凭证调用。",
        responses = {
            @ApiResponse(responseCode = "200", description = "取消成功"),
            @ApiResponse(responseCode = "401", description = "未提供任何有效凭证",
                         content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "当前状态不允许取消",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @SecurityRequirement(name = "ApiKeyAuth")
    @SecurityRequirement(name = "BearerAuth")
    @PostMapping("/batches/{settlementId}/cancel")
    public ResponseEntity<BatchSettlementResponse> cancelBatch(
            @Parameter(description = "系统结算 ID", example = "settle-2024-04-25-9001")
            @PathVariable String settlementId,
            @Parameter(description = "取消原因", example = "DUPLICATE_BATCH")
            @RequestParam(required = false) String reason
    ) {
        BatchSettlementResponse resp = BatchSettlementResponse.builder()
                .settlementId(settlementId)
                .status(SettlementStatus.CANCELED)
                .completedAt(Instant.now())
                .build();
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "查询商户余额快照",
        description = "返回多币种账户视图、子账本拆分、出款限额与近期动账。" +
                      "用于商户后台首页和对账场景。"
    )
    @GetMapping("/merchants/{merchantId}/balance")
    public ResponseEntity<MerchantBalanceSnapshot> getBalanceSnapshot(
            @Parameter(description = "商户号", example = "merchant-77001")
            @PathVariable String merchantId,
            @Parameter(description = "快照截止日期（不传则取当下）", example = "2024-04-25")
            @RequestParam(required = false) LocalDate asOf,
            @Parameter(description = "近期动账返回条数（最大 100）", example = "20")
            @RequestParam(required = false, defaultValue = "20") int recentLimit
    ) {
        CurrencyAccount cny = CurrencyAccount.builder()
                .currency("CNY")
                .available(new Money(new BigDecimal("128456.78"), "CNY"))
                .pending(new Money(new BigDecimal("12920.50"), "CNY"))
                .frozen(new Money(new BigDecimal("0.00"), "CNY"))
                .subLedgers(List.of(
                        SubLedger.builder()
                                .purpose(SubLedgerPurpose.SETTLEABLE)
                                .balance(new Money(new BigDecimal("128456.78"), "CNY"))
                                .lastUpdatedAt(Instant.now())
                                .build(),
                        SubLedger.builder()
                                .purpose(SubLedgerPurpose.IN_TRANSIT)
                                .balance(new Money(new BigDecimal("12920.50"), "CNY"))
                                .lastUpdatedAt(Instant.now())
                                .build(),
                        SubLedger.builder()
                                .purpose(SubLedgerPurpose.CHARGEBACK_RESERVE)
                                .balance(new Money(new BigDecimal("5000.00"), "CNY"))
                                .lastUpdatedAt(Instant.now().minusSeconds(86400))
                                .build()
                ))
                .build();

        CurrencyAccount usd = CurrencyAccount.builder()
                .currency("USD")
                .available(new Money(new BigDecimal("8421.30"), "USD"))
                .pending(new Money(new BigDecimal("0.00"), "USD"))
                .frozen(new Money(new BigDecimal("0.00"), "USD"))
                .subLedgers(List.of(
                        SubLedger.builder()
                                .purpose(SubLedgerPurpose.SETTLEABLE)
                                .balance(new Money(new BigDecimal("8421.30"), "USD"))
                                .lastUpdatedAt(Instant.now())
                                .build()
                ))
                .build();

        PayoutLimits limits = PayoutLimits.builder()
                .dailyLimit(new Money(new BigDecimal("500000.00"), "CNY"))
                .dailyUsed(new Money(new BigDecimal("12920.50"), "CNY"))
                .monthlyLimit(new Money(new BigDecimal("5000000.00"), "CNY"))
                .monthlyUsed(new Money(new BigDecimal("428765.32"), "CNY"))
                .singleTransactionLimit(new Money(new BigDecimal("100000.00"), "CNY"))
                .build();

        RecentMovement movement = RecentMovement.builder()
                .type("CREDIT")
                .amount(new Money(new BigDecimal("1290.50"), "CNY"))
                .counterparty("buyer-991")
                .reference("ORD-20240425-9001")
                .occurredAt(Instant.now().minusSeconds(900))
                .build();

        MerchantBalanceSnapshot snapshot = MerchantBalanceSnapshot.builder()
                .merchantId(merchantId)
                .snapshotId("snap-2024-04-25-9001")
                .accounts(List.of(cny, usd))
                .limits(limits)
                .recentMovements(List.of(movement))
                .generatedAt(Instant.now())
                .build();
        return ResponseEntity.ok(snapshot);
    }
}
