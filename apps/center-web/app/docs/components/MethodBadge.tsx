// HTTP 方法 pill 徽章
// 极简中性色板：GET/PUT/HEAD/OPTIONS 浅灰，POST 黑底白字，PATCH 紫色突出，DELETE 保留红色功能信号

type Size = "sm" | "md" | "lg";

const METHOD_STYLES: Record<string, { bg: string; text: string; border?: string }> = {
  GET:     { bg: "#F0F0F0", text: "#444444", border: "#D0D0D0" },
  POST:    { bg: "#111111", text: "#FFFFFF" },
  PUT:     { bg: "#F5F5F5", text: "#333333", border: "#D0D0D0" },
  PATCH:   { bg: "rgba(124,58,237,0.08)", text: "#7C3AED" },
  DELETE:  { bg: "#FFF0F0", text: "#CC0000" },
  HEAD:    { bg: "#F0F0F0", text: "#666666", border: "#D0D0D0" },
  OPTIONS: { bg: "#F0F0F0", text: "#666666", border: "#D0D0D0" },
};

const SIZE_CLASSES: Record<Size, string> = {
  sm: "px-2.5 py-1 text-[10px] min-w-[48px]",
  md: "px-3 py-1 text-[11px] min-w-[56px]",
  lg: "px-3.5 py-1.5 text-[12px] min-w-[68px]",
};

export function MethodBadge({ method, size = "md" }: { method: string; size?: Size }) {
  const upper = method.toUpperCase();
  const style = METHOD_STYLES[upper] ?? { bg: "#F0F0F0", text: "#666666", border: "#D0D0D0" };
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded-[999px] font-mono font-semibold uppercase tracking-[0.08em] ${SIZE_CLASSES[size]}`}
      style={{
        backgroundColor: style.bg,
        color: style.text,
        border: `1px solid ${style.border ?? "transparent"}`,
      }}
    >
      {upper}
    </span>
  );
}
