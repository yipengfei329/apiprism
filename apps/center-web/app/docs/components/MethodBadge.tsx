// HTTP 方法 pill 徽章
// 每个 method 使用 CSS 变量三元组（bg/text/border），在 :root 和 .dark 中分别定义，
// 两个主题下都是低饱和深色版，兼顾可读性与 Linear/Radix 视觉节制。

type Size = "sm" | "md" | "lg";

type MethodTokens = { bg: string; text: string; border: string };

const METHOD_STYLES: Record<string, MethodTokens> = {
  GET: {
    bg: "var(--method-get-bg)",
    text: "var(--method-get-text)",
    border: "var(--method-get-border)",
  },
  POST: {
    bg: "var(--method-post-bg)",
    text: "var(--method-post-text)",
    border: "var(--method-post-border)",
  },
  PUT: {
    bg: "var(--method-put-bg)",
    text: "var(--method-put-text)",
    border: "var(--method-put-border)",
  },
  PATCH: {
    bg: "var(--method-patch-bg)",
    text: "var(--method-patch-text)",
    border: "var(--method-patch-border)",
  },
  DELETE: {
    bg: "var(--method-delete-bg)",
    text: "var(--method-delete-text)",
    border: "var(--method-delete-border)",
  },
  HEAD: {
    bg: "var(--method-head-bg)",
    text: "var(--method-head-text)",
    border: "var(--method-head-border)",
  },
  OPTIONS: {
    bg: "var(--method-options-bg)",
    text: "var(--method-options-text)",
    border: "var(--method-options-border)",
  },
};

const FALLBACK: MethodTokens = {
  bg: "var(--method-head-bg)",
  text: "var(--method-head-text)",
  border: "var(--method-head-border)",
};

const SIZE_CLASSES: Record<Size, string> = {
  sm: "px-2.5 py-1 text-[10px] min-w-[48px]",
  md: "px-3 py-1 text-[11px] min-w-[56px]",
  lg: "px-3.5 py-1.5 text-[12px] min-w-[68px]",
};

export function MethodBadge({ method, size = "md" }: { method: string; size?: Size }) {
  const upper = method.toUpperCase();
  const style = METHOD_STYLES[upper] ?? FALLBACK;
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded-[999px] font-mono font-semibold uppercase tracking-[0.08em] ${SIZE_CLASSES[size]}`}
      style={{
        backgroundColor: style.bg,
        color: style.text,
        border: `1px solid ${style.border}`,
      }}
    >
      {upper}
    </span>
  );
}
