/**
 * 安全渲染 HTML 片段的内联组件。
 *
 * OpenAPI description 可能包含 Javadoc 生成的 HTML 标签（<p>、<ul>、<code> 等），
 * 这里用白名单过滤后通过 dangerouslySetInnerHTML 渲染，避免原样显示标签文本。
 */

// 允许保留的 HTML 标签白名单（覆盖 Javadoc 常见标签）
const ALLOWED_TAGS = new Set([
  "p", "br", "b", "i", "em", "strong", "code", "pre",
  "ul", "ol", "li", "a", "span", "sub", "sup", "hr",
  "h1", "h2", "h3", "h4", "h5", "h6",
  "table", "thead", "tbody", "tr", "th", "td",
  "blockquote", "dl", "dt", "dd",
]);

// 允许保留的属性白名单
const ALLOWED_ATTRS = new Set(["href", "target", "rel", "class", "title"]);

/**
 * 简易 HTML 净化：移除非白名单标签和属性，防止 XSS。
 * 不引入外部依赖，满足 Javadoc HTML 子集的渲染需求。
 */
function sanitize(html: string): string {
  return html.replace(/<\/?([a-zA-Z][a-zA-Z0-9]*)\b([^>]*)?\/?>/g, (match, tag: string, attrs: string | undefined) => {
    const lower = tag.toLowerCase();
    if (!ALLOWED_TAGS.has(lower)) return "";

    // 关闭标签直接保留
    if (match.startsWith("</")) return `</${lower}>`;

    // 过滤属性
    let safeAttrs = "";
    if (attrs) {
      const attrRegex = /([a-zA-Z-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')/g;
      let attrMatch;
      while ((attrMatch = attrRegex.exec(attrs)) !== null) {
        const attrName = attrMatch[1].toLowerCase();
        const attrValue = attrMatch[2] ?? attrMatch[3];
        if (!ALLOWED_ATTRS.has(attrName)) continue;
        // href 只允许 http/https/mailto 和相对路径，阻止 javascript:
        if (attrName === "href" && /^\s*javascript:/i.test(attrValue)) continue;
        safeAttrs += ` ${attrName}="${attrValue}"`;
      }
      // a 标签自动补 target 和 rel
      if (lower === "a" && !safeAttrs.includes("target=")) {
        safeAttrs += ' target="_blank" rel="noopener noreferrer"';
      }
    }

    const selfClosing = match.endsWith("/>");
    return `<${lower}${safeAttrs}${selfClosing ? " /" : ""}>`;
  });
}

/** 检测字符串是否包含 HTML 标签 */
function containsHtml(text: string): boolean {
  return /<[a-zA-Z][^>]*>/.test(text);
}

interface HtmlTextProps {
  /** 可能包含 HTML 的文本内容 */
  text: string;
  className?: string;
  /** 渲染的外层标签，默认 span */
  as?: "p" | "span" | "div";
}

export function HtmlText({ text, className, as: Tag = "span" }: HtmlTextProps) {
  if (!containsHtml(text)) {
    return <Tag className={className}>{text}</Tag>;
  }
  return (
    <Tag
      className={className}
      dangerouslySetInnerHTML={{ __html: sanitize(text) }}
    />
  );
}
