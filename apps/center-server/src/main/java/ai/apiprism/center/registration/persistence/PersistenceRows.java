package ai.apiprism.center.registration.persistence;

import java.time.Instant;

/**
 * 中心规范化模型各表的行数据记录（JDBC 层用）。
 * 聚合在一个文件以减少样板；每个 record 对应一张 V3 表。
 */
public final class PersistenceRows {

    private PersistenceRows() {
    }

    public record ServiceRow(
            String id,
            String name,
            String defaultLocale,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record EnvironmentRow(
            String id,
            String code) {
    }

    public record DeploymentRow(
            String id,
            String serviceId,
            String environmentId,
            String version,
            String adapterType,
            String specFormat,
            String serverUrlsJson,
            String specHash,
            String warningsJson,
            String extensionsJson,
            Instant lastRegisteredAt) {
    }

    public record TagRow(
            String id,
            String serviceId,
            String name,
            String slug) {
    }

    public record OperationRow(
            String id,
            String serviceId,
            String operationKey,
            String method,
            String path,
            String definitionHash,
            String securityRequirementsJson,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record OperationParameterRow(
            String id,
            String operationId,
            String location,
            String name,
            boolean required,
            String schemaJson,
            int sortOrder) {
    }

    public record OperationRequestBodyRow(
            String id,
            String operationId,
            String contentType,
            boolean required,
            String schemaJson) {
    }

    public record OperationResponseRow(
            String id,
            String operationId,
            String statusCode,
            String contentType,
            String schemaJson) {
    }

    public record OperationDefinitionVersionRow(
            String id,
            String operationId,
            String definitionHash,
            String definitionJson,
            String sourceDeploymentId,
            Instant createdAt) {
    }
}
