package ai.apiprism.center.config;

import ai.apiprism.openapi.OpenApiNormalizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * center-server 核心 Bean 配置。
 */
@Configuration
@EnableConfigurationProperties(CenterProperties.class)
public class CenterConfiguration {

    @Bean
    public OpenApiNormalizer openApiNormalizer() {
        return new OpenApiNormalizer();
    }
}
