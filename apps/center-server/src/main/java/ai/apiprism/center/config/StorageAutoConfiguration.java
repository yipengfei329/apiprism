package ai.apiprism.center.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 存储层自动配置：根据 apiprism.storage.data-dir 程序化创建 DataSource 和目录结构。
 * 不暴露 spring.datasource.* 配置，使用者只需设置 apiprism.storage.data-dir（可选）。
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(StorageAutoConfiguration.class);

    @Bean
    public DataSource dataSource(StorageProperties properties) throws IOException {
        Path dataDir = Path.of(properties.getDataDir());

        // 确保子目录存在
        Files.createDirectories(dataDir.resolve("db"));
        Files.createDirectories(dataDir.resolve("snapshots"));
        Files.createDirectories(dataDir.resolve("raw-specs"));

        String jdbcUrl = "jdbc:h2:file:" + dataDir.resolve("db/center")
                + ";AUTO_SERVER=TRUE;MODE=MySQL";

        log.info("Initializing H2 storage at {}", dataDir);

        return DataSourceBuilder.create()
                .url(jdbcUrl)
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();
    }
}
