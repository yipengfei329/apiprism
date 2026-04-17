package ai.apiprism.center.localization;

/**
 * 解析 Accept-Language 头，仅取首选 locale。
 * 例：Accept-Language: "en-US,en;q=0.9,zh;q=0.5" → "en-US"。
 * 头缺失或格式非法时返回 null，交由上层回落到服务默认 locale。
 */
public final class AcceptLanguageParser {

    private AcceptLanguageParser() {
    }

    public static String pickPrimary(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isBlank()) {
            return null;
        }
        String first = acceptLanguage.split(",", 2)[0].trim();
        int semi = first.indexOf(';');
        if (semi > 0) {
            first = first.substring(0, semi).trim();
        }
        return first.isEmpty() ? null : first;
    }
}
