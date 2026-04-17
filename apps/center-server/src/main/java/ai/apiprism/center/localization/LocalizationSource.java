package ai.apiprism.center.localization;

/**
 * 本地化内容来源。adapter 代表适配器注册时带过来的原始文案；
 * center 代表中心运营人员手动编辑/翻译的文案。同一 (entity, field, locale) 下
 * center 优先于 adapter，适配器刷新不会覆盖中心改写。
 */
public enum LocalizationSource {

    ADAPTER("adapter"),
    CENTER("center");

    private final String code;

    LocalizationSource(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static LocalizationSource fromCode(String code) {
        for (LocalizationSource s : values()) {
            if (s.code.equals(code)) {
                return s;
            }
        }
        throw new IllegalArgumentException("Unknown localization source: " + code);
    }
}
