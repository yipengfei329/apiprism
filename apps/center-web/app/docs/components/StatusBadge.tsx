// HTTP 状态码徽章（语义色）

export function StatusBadge({ code, inverted = false }: { code: string; inverted?: boolean }) {
  const n = parseInt(code, 10);
  let bg = "#F5F5F5", text = "#444444";
  if (n >= 200 && n < 300) { bg = "#F0FDF4"; text = "#15803D"; }
  else if (n >= 300 && n < 400) { bg = "#F5F5F5"; text = "#444444"; }
  else if (n >= 400 && n < 500) { bg = "#FFF7ED"; text = "#9A3412"; }
  else if (n >= 500) { bg = "#FEF2F2"; text = "#991B1B"; }
  return (
    <span
      className="inline-flex items-center rounded-md px-2 py-0.5 font-mono text-[12px] font-semibold"
      style={inverted ? { backgroundColor: "rgba(255,255,255,0.16)", color: "#FFFFFF" } : { backgroundColor: bg, color: text }}
    >
      {code}
    </span>
  );
}
