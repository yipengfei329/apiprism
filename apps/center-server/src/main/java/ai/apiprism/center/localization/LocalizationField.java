package ai.apiprism.center.localization;

/**
 * 可本地化字段名常量，值写入 content_localizations.field。
 * 用常量替代魔法字符串，避免读写两端拼错。
 */
public final class LocalizationField {

    public static final String TITLE = "title";
    public static final String SUMMARY = "summary";
    public static final String DESCRIPTION = "description";

    private LocalizationField() {
    }
}
