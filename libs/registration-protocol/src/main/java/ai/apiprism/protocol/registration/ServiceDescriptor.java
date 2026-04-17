package ai.apiprism.protocol.registration;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * 服务在注册边界上的基础描述信息。
 */
@Value
@Builder(toBuilder = true)
public class ServiceDescriptor {

    /**
     * 服务的稳定名称，是中心侧查询与覆盖注册的主键之一。
     */
    @NonNull
    String name;

    /**
     * 服务所属环境，例如 dev、test、prod。
     */
    @NonNull
    String environment;

    /**
     * 展示给人看的标题，允许为空，中心会在归一化阶段回退。
     */
    String title;

    /**
     * 业务声明的版本号，允许为空，中心会在归一化阶段回退。
     */
    String version;

    /**
     * 服务暴露出来的访问地址列表。
     */
    @Singular(value = "serverUrl", ignoreNullCollections = true)
    List<String> serverUrls;

    /**
     * 产生本次注册的适配器类型。
     */
    String adapterType;

    /**
     * 适配器提交内容的默认 locale（BCP-47，如 zh-CN、en-US）。
     * 中心据此把 title/summary/description 写入 content_localizations 的对应 locale 行，
     * 并作为读取时回落的默认语言。留空时中心按全局配置回落。
     */
    String defaultLocale;
}
