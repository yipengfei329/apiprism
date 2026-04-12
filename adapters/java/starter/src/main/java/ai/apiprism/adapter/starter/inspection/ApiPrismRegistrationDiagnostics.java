package ai.apiprism.adapter.starter.inspection;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 映射检查结果，记录运行时映射数量、文档化操作数量及未文档化的映射列表。
 */
@Getter
@Builder
public class ApiPrismRegistrationDiagnostics {

    /** 运行时检测到的映射总数 */
    private final int mappingCount;

    /** 在 OpenAPI 文档中有记录的操作数量 */
    private final int documentedOperationCount;

    /** 未在 OpenAPI 文档中记录的映射路径列表（不可变） */
    private final List<String> undocumentedMappings;

    /** 是否存在未文档化的映射 */
    public boolean hasUndocumentedMappings() {
        return undocumentedMappings != null && !undocumentedMappings.isEmpty();
    }
}
