// HTTP 状态码徽章（语义色）

export function StatusBadge({ code }: { code: string }) {
  const n = parseInt(code, 10);
  let bg = "#F2F2F7", text = "#3C3C43";
  if (n >= 200 && n < 300) { bg = "#ECFDF5"; text = "#047857"; }
  else if (n >= 300 && n < 400) { bg = "#EEF4FF"; text = "#0063CC"; }
  else if (n >= 400 && n < 500) { bg = "#FFF7ED"; text = "#C2410C"; }
  else if (n >= 500) { bg = "#FEF2F2"; text = "#B91C1C"; }
  return (
    <span
      className="inline-flex items-center rounded-md px-2 py-0.5 font-mono text-[12px] font-semibold"
      style={{ backgroundColor: bg, color: text }}
    >
      {code}
    </span>
  );
}
