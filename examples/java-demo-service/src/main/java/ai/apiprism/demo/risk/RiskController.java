package ai.apiprism.demo.risk;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 风控决策开放接口。
 * <p>
 * 所有接口要求在请求头 {@code X-Api-Key} 中携带合作伙伴 API Key（控制台申请）。
 * <p>
 * 覆盖测试场景：
 * <ul>
 *   <li>顶层 + 五层嵌套请求体（主体 / 设备 → 网络 / 行为序列 / 评估选项）</li>
 *   <li>响应包含 List + Map&lt;String, Object&gt; 混合结构（factors / ruleHits / evidence）</li>
 *   <li>使用 {@code @SecurityRequirement} 声明 API Key 鉴权</li>
 *   <li>Header 参数（X-Tenant-Id / Idempotency-Key）独立暴露在 OpenAPI 中</li>
 * </ul>
 */
@RestController
@RequestMapping("/risk")
@Tag(name = "风险评估", description = "面向开放平台的风控决策与人工复核接口（需要 API Key）")
@SecurityRequirement(name = "ApiKeyAuth")
public class RiskController {

    @Operation(
        summary = "提交风险评估",
        description = "对单笔业务请求进行实时风控评估。返回终判结果、评分、命中规则与决策建议。" +
                      "幂等性由 Idempotency-Key 头保证，相同 key 在 24 小时内返回首次结果。",
        responses = {
            @ApiResponse(responseCode = "200", description = "评估成功"),
            @ApiResponse(responseCode = "401", description = "API Key 缺失或无效",
                         content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "调用方限流，请稍后重试",
                         content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "503", description = "评分服务降级（具体行为由 fallbackStrategy 决定）",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @PostMapping("/evaluations")
    public ResponseEntity<RiskEvaluationResponse> evaluate(
            @Parameter(description = "幂等键（建议 UUID），24 小时内复用同 key 返回首次结果",
                       example = "idem-2024-04-25-9001")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Parameter(description = "租户 ID，覆盖请求体中的 tenantId（沙箱测试用）",
                       example = "tenant-merchant-77")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantHeader,
            @RequestBody RiskEvaluationRequest request
    ) {
        // Demo stub：构造一个高风险 + 建议二次验证的样例
        RiskFactor proxy = RiskFactor.builder()
                .code("network.proxy_detected")
                .label("代理网络")
                .severity(Severity.HIGH)
                .contribution(0.32)
                .description("本次请求来源 IP 命中 30 天内的代理库")
                .evidence(Map.of("ip", "203.0.113.42", "asn", 4134))
                .build();
        RiskFactor velocity = RiskFactor.builder()
                .code("velocity.same_device_multi_account")
                .label("同设备多账号")
                .severity(Severity.MEDIUM)
                .contribution(0.18)
                .description("近 5 分钟该设备登录了 4 个不同账号")
                .evidence(Map.of("window", "5m", "distinctSubjects", 4))
                .build();

        RiskRuleHit ruleHit = RiskRuleHit.builder()
                .ruleId("rule-velocity-001")
                .name("5 分钟内同设备多账号下单")
                .ruleSetId("rs-china-anti-fraud")
                .version("v3.2.1")
                .weight(0.5)
                .matchedFields(List.of("device.deviceId", "transaction.amount.value"))
                .snapshot(Map.of("window", "5m", "distinctSubjects", 4))
                .build();

        Recommendation rec = Recommendation.builder()
                .action("STEP_UP_AUTH")
                .requiredVerifications(List.of("SMS_OTP", "FACE_RECOGNITION"))
                .alternativeChannels(List.of("BANK_TRANSFER"))
                .expiresAt(Instant.now().plusSeconds(900))
                .reviewerNote(null)
                .build();

        RiskEvaluationResponse resp = RiskEvaluationResponse.builder()
                .evaluationId("eval-2024-04-25-9001")
                .requestId(request.getRequestId())
                .decision(RiskDecision.CHALLENGE)
                .riskScore(72.5)
                .confidence(0.87)
                .factors(List.of(proxy, velocity))
                .ruleHits(List.of(ruleHit))
                .recommendation(rec)
                .evaluatedAt(Instant.now())
                .latencyMs(87L)
                .modelTrace("model:v3.2#rules:rs-china-anti-fraud@2024-04-20")
                .build();
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "查询评估结果",
        description = "幂等查询：根据评估 ID 返回当时的决策快照与可解释性信息。",
        responses = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "API Key 缺失或无效",
                         content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "评估不存在或已过期",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @GetMapping("/evaluations/{evaluationId}")
    public ResponseEntity<RiskEvaluationResponse> getEvaluation(
            @Parameter(description = "评估 ID", example = "eval-2024-04-25-9001")
            @PathVariable String evaluationId
    ) {
        RiskEvaluationResponse resp = RiskEvaluationResponse.builder()
                .evaluationId(evaluationId)
                .requestId("req-2024-04-25-9001")
                .decision(RiskDecision.ALLOW)
                .riskScore(18.4)
                .confidence(0.93)
                .factors(List.of())
                .ruleHits(List.of())
                .recommendation(Recommendation.builder()
                        .action("PROCEED")
                        .expiresAt(Instant.now().plusSeconds(900))
                        .build())
                .evaluatedAt(Instant.now())
                .latencyMs(64L)
                .modelTrace("model:v3.2#rules:rs-default@2024-04-20")
                .build();
        return ResponseEntity.ok(resp);
    }

    @Operation(
        summary = "提交人工复核结果",
        description = "对处于 REVIEW 状态的评估单提交人工决策。系统会写入审计日志并联动" +
                      "钱包冻结/解冻、邮件/站内信等下游动作。",
        responses = {
            @ApiResponse(responseCode = "200", description = "复核成功"),
            @ApiResponse(responseCode = "401", description = "API Key 缺失或无效",
                         content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "工单已被其他复核员处理",
                         content = @Content(schema = @Schema(hidden = true)))
        }
    )
    @PostMapping("/cases/{caseId}/review")
    public ResponseEntity<ManualReviewResponse> submitReview(
            @Parameter(description = "审核单号", example = "case-2024-04-25-0042")
            @PathVariable String caseId,
            @RequestBody ManualReviewRequest request
    ) {
        ManualReviewResponse resp = ManualReviewResponse.builder()
                .caseId(caseId)
                .evaluationId("eval-2024-04-25-9001")
                .finalDecision(request.getFinalDecision())
                .reviewerId(request.getReviewerId())
                .decidedAt(Instant.now())
                .handlingSeconds(118L)
                .downstreamSignals("WALLET_UNFROZEN,EMAIL_SENT")
                .build();
        return ResponseEntity.ok(resp);
    }
}
