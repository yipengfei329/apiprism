package ai.apiprism.adapter.starter.registration;

import ai.apiprism.adapter.starter.ApiPrismProperties;
import ai.apiprism.adapter.starter.inspection.ApiPrismRegistrationDiagnostics;
import ai.apiprism.adapter.starter.openapi.ApiPrismOpenApiDocument;
import ai.apiprism.protocol.registration.ApiRegistrationRequest;
import ai.apiprism.protocol.registration.ServiceDescriptor;
import ai.apiprism.protocol.registration.SpecPayload;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 将已解析的服务元数据、OpenAPI 文档和映射诊断结果组装为 {@link ApiRegistrationRequest}。
 */
public class RegistrationRequestFactory {

    private final ApiPrismProperties properties;

    public RegistrationRequestFactory(ApiPrismProperties properties) {
        this.properties = properties;
    }

    public ApiRegistrationRequest build(
            ServiceMetadata metadata,
            ApiPrismOpenApiDocument document,
            ApiPrismRegistrationDiagnostics diagnostics
    ) {
        return ApiRegistrationRequest.builder()
                .service(ServiceDescriptor.builder()
                        .name(metadata.projectName())
                        .environment(metadata.environment())
                        .serverUrls(metadata.serverUrls())
                        .adapterType("spring-boot-starter")
                        .defaultLocale(properties.getDefaultLocale())
                        .build())
                .spec(SpecPayload.builder()
                        .format(document.getFormat())
                        .content(document.getContent())
                        .build())
                .extensions(buildExtensions(document, diagnostics))
                .build();
    }

    private Map<String, Object> buildExtensions(
            ApiPrismOpenApiDocument document,
            ApiPrismRegistrationDiagnostics diagnostics
    ) {
        Map<String, Object> javaExtension = new LinkedHashMap<>();
        javaExtension.put("framework", "spring-boot");
        javaExtension.put("openapiPath", properties.getOpenapiPath());
        javaExtension.put("openapiSource", document.getSource());
        javaExtension.put("mappingCount", diagnostics.getMappingCount());
        javaExtension.put("documentedOperationCount", diagnostics.getDocumentedOperationCount());
        if (diagnostics.hasUndocumentedMappings()) {
            List<String> sample = diagnostics.getUndocumentedMappings().stream().limit(20).toList();
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("total", diagnostics.getUndocumentedMappings().size());
            summary.put("sample", sample);
            summary.put("truncated", diagnostics.getUndocumentedMappings().size() > sample.size());
            javaExtension.put("undocumentedMappings", summary);
        }

        Map<String, Object> extensions = new LinkedHashMap<>();
        extensions.put("ai.apiprism/java", javaExtension);
        return extensions;
    }
}
