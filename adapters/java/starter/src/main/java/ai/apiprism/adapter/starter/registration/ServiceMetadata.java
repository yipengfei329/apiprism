package ai.apiprism.adapter.starter.registration;

import java.util.List;

/**
 * 已解析的服务身份信息，在 {@link ServiceMetadataResolver} 和 {@link RegistrationRequestFactory} 之间传递。
 */
record ServiceMetadata(
        String projectName,
        String environment,
        List<String> serverUrls
) {
}
