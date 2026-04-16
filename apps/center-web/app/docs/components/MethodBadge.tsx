// HTTP 方法 pill 徽章
// pill 圆角（9999px），Geist Mono uppercase，每个方法对应功能性色彩
// 色板参考 Apple System Colors：淡色背景 + 深色文字，低饱和度，视觉清晰

type Size = "sm" | "md" | "lg";

// Apple 风格：柔和背景 + 深色主调文字（避免过饱和）
const METHOD_STYLES: Record<string, { bg: string; text: string }> = {
  GET:     { bg: "#E3EEFF", text: "#0055CC" },
  POST:    { bg: "#E4F7EC", text: "#0A6B30" },
  PUT:     { bg: "#FFF4DF", text: "#7A3B00" },
  PATCH:   { bg: "#F3EAFF", text: "#5B1FA6" },
  DELETE:  { bg: "#FFEBEB", text: "#B91C1C" },
  HEAD:    { bg: "#E0F4FF", text: "#00619E" },
  OPTIONS: { bg: "#EEEEF0", text: "#3C3C43" },
};

const SIZE_CLASSES: Record<Size, string> = {
  // sm：侧边栏内使用，适当加宽以保证可读性
  sm: "px-2.5 py-1 text-[10px] min-w-[48px]",
  md: "px-3 py-1 text-[11px] min-w-[56px]",
  lg: "px-3.5 py-1.5 text-[12px] min-w-[68px]",
};

export function MethodBadge({ method, size = "md" }: { method: string; size?: Size }) {
  const upper = method.toUpperCase();
  const style = METHOD_STYLES[upper] ?? { bg: "#EEEEF0", text: "#3C3C43" };
  return (
    <span
      className={`inline-flex shrink-0 items-center justify-center rounded-[999px] border border-black/5 font-mono font-semibold uppercase tracking-[0.08em] shadow-[inset_0_1px_0_rgba(255,255,255,0.45)] ${SIZE_CLASSES[size]}`}
      style={{ backgroundColor: style.bg, color: style.text }}
    >
      {upper}
    </span>
  );
}
