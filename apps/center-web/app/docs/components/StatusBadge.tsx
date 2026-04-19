// HTTP 状态码徽章（语义色）

export function StatusBadge({ code, inverted = false }: { code: string; inverted?: boolean }) {
  const n = parseInt(code, 10);
  let bg = "var(--status-3xx-bg)";
  let text = "var(--status-3xx-text)";
  if (n >= 200 && n < 300) {
    bg = "var(--status-2xx-bg)";
    text = "var(--status-2xx-text)";
  } else if (n >= 300 && n < 400) {
    bg = "var(--status-3xx-bg)";
    text = "var(--status-3xx-text)";
  } else if (n >= 400 && n < 500) {
    bg = "var(--status-4xx-bg)";
    text = "var(--status-4xx-text)";
  } else if (n >= 500) {
    bg = "var(--status-5xx-bg)";
    text = "var(--status-5xx-text)";
  }
  return (
    <span
      className="inline-flex items-center rounded-md px-2 py-0.5 font-mono text-[12px] font-semibold"
      style={
        inverted
          ? { backgroundColor: "var(--chip-on-inverse-bg)", color: "var(--text-on-inverse)" }
          : { backgroundColor: bg, color: text }
      }
    >
      {code}
    </span>
  );
}
