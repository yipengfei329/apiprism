package ai.apiprism.center.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存储配置属性，使用者仅需关注 apiprism.storage.data-dir 即可。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "apiprism.storage")
public class StorageProperties {

    /**
     * 数据文件根目录，默认 ${user.home}/.apiprism/data。
     * 目录下会自动创建 db/、snapshots/、raw-specs/ 子目录。
     */
    private String dataDir = System.getProperty("user.home") + "/.apiprism/data";
}
