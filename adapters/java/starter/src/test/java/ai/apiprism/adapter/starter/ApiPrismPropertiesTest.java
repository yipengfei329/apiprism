package ai.apiprism.adapter.starter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiPrismPropertiesTest {

    @Test
    void usesReasonableDefaults() {
        ApiPrismProperties properties = new ApiPrismProperties();

        assertTrue(properties.isEnabled());
        assertTrue(properties.isRegisterOnStartup());
        assertEquals("/v3/api-docs", properties.getOpenapiPath());
        // environment 无默认值，由 Listener 在运行时从 Spring profile 推断
        assertNull(properties.getEnv());
        // project-name 无默认值，由 Listener 在运行时回退 spring.application.name
        assertNull(properties.getProjectName());
        // server-urls 默认空列表，由 Listener 回退本地地址
        assertTrue(properties.getServerUrls().isEmpty());
    }
}
