"use client";

import { type CSSProperties, useEffect, useState } from "react";
import { useTheme } from "next-themes";
import JsonView from "@uiw/react-json-view";
import { TriangleArrow } from "@uiw/react-json-view/triangle-arrow";
import { CopyButton } from "./CopyButton";

/* ── 浅色主题（Vercel 极简风格） ────────────────── */
const appleLight = {
  "--w-rjv-background-color": "transparent",
  "--w-rjv-color": "#1C1C1E",
  "--w-rjv-key-string": "#1C1C1E",
  "--w-rjv-type-string-color": "#0A6B30",
  "--w-rjv-type-int-color": "#0D9488",
  "--w-rjv-type-float-color": "#0D9488",
  "--w-rjv-type-boolean-color": "#C0148A",
  "--w-rjv-type-null-color": "#8E8E93",
  "--w-rjv-type-undefined-color": "#8E8E93",
  "--w-rjv-type-nan-color": "#8E8E93",
  "--w-rjv-type-date-color": "#636366",
  "--w-rjv-type-url-color": "#0D9488",
  "--w-rjv-curlybraces-color": "#3C3C43",
  "--w-rjv-brackets-color": "#3C3C43",
  "--w-rjv-colon-color": "#8E8E93",
  "--w-rjv-quotes-color": "#3C3C43",
  "--w-rjv-quotes-string-color": "#0A6B30",
  "--w-rjv-arrow-color": "#8E8E93",
  "--w-rjv-ellipsis-color": "#636366",
  "--w-rjv-line-color": "rgba(60, 60, 67, 0.08)",
  "--w-rjv-info-color": "rgba(142, 142, 147, 0.55)",
  "--w-rjv-copied-color": "#0D9488",
  "--w-rjv-copied-success-color": "#0A6B30",
  "--w-rjv-font-family":
    "var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace)",
  fontSize: 13,
} as CSSProperties;

/* ── 深色主题（匹配 #0f0f11 基调，accent 用 teal-400） ─── */
const appleDark = {
  "--w-rjv-background-color": "transparent",
  "--w-rjv-color": "rgba(255,255,255,0.85)",
  "--w-rjv-key-string": "rgba(255,255,255,0.92)",
  "--w-rjv-type-string-color": "#7DD3A0",
  "--w-rjv-type-int-color": "#2DD4BF",
  "--w-rjv-type-float-color": "#2DD4BF",
  "--w-rjv-type-boolean-color": "#E091C9",
  "--w-rjv-type-null-color": "rgba(255,255,255,0.38)",
  "--w-rjv-type-undefined-color": "rgba(255,255,255,0.38)",
  "--w-rjv-type-nan-color": "rgba(255,255,255,0.38)",
  "--w-rjv-type-date-color": "rgba(255,255,255,0.6)",
  "--w-rjv-type-url-color": "#2DD4BF",
  "--w-rjv-curlybraces-color": "rgba(255,255,255,0.55)",
  "--w-rjv-brackets-color": "rgba(255,255,255,0.55)",
  "--w-rjv-colon-color": "rgba(255,255,255,0.38)",
  "--w-rjv-quotes-color": "rgba(255,255,255,0.55)",
  "--w-rjv-quotes-string-color": "#7DD3A0",
  "--w-rjv-arrow-color": "rgba(255,255,255,0.38)",
  "--w-rjv-ellipsis-color": "rgba(255,255,255,0.5)",
  "--w-rjv-line-color": "rgba(255,255,255,0.06)",
  "--w-rjv-info-color": "rgba(255,255,255,0.22)",
  "--w-rjv-copied-color": "#2DD4BF",
  "--w-rjv-copied-success-color": "#7DD3A0",
  "--w-rjv-font-family":
    "var(--font-mono, ui-monospace, SFMono-Regular, Menlo, monospace)",
  fontSize: 13,
} as CSSProperties;

const THEMES = { light: appleLight, dark: appleDark } as const;

/* ── JsonPreview 组件 ─────────────────────────────────────── */
export interface JsonPreviewProps {
  /** 要预览的 JSON 数据 */
  data: object;
  /** 颜色主题，默认跟随全局主题；传 light/dark 可强制 */
  variant?: "light" | "dark";
  /** 默认展开层级，默认 2 */
  defaultExpandDepth?: number;
  /** 是否显示顶部复制按钮，默认 true */
  copyable?: boolean;
  /** 是否显示数据类型标记，默认 false */
  showDataTypes?: boolean;
  /** 是否显示对象/数组元素数量，默认 true */
  showObjectSize?: boolean;
  /** 容器最大高度（px），默认 400 */
  maxHeight?: number;
  /** 额外的容器 className */
  className?: string;
}

export function JsonPreview({
  data,
  variant,
  defaultExpandDepth = 2,
  copyable = true,
  showDataTypes = false,
  showObjectSize = true,
  maxHeight = 400,
  className = "",
}: JsonPreviewProps) {
  const { resolvedTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
  }, []);

  // 未挂载前（SSR / 水合第一帧）固定使用 light，避免闪烁
  const active = variant ?? (mounted && resolvedTheme === "dark" ? "dark" : "light");
  const theme = THEMES[active];

  const containerBg =
    active === "dark" ? "bg-[var(--sidebar-bg)]" : "bg-[var(--bg-subtle)]/60";

  return (
    <div className={`relative rounded-2xl ${containerBg} ${className}`}>
      {/* 顶栏：复制按钮 */}
      {copyable && (
        <div className="flex justify-end px-3 pt-2.5 pb-0">
          <CopyButton text={JSON.stringify(data, null, 2)} variant={active} />
        </div>
      )}

      {/* JSON 树 */}
      <div
        className="overflow-auto px-5 pb-5"
        style={{
          maxHeight,
          paddingTop: copyable ? 4 : 20,
        }}
      >
        <JsonView
          value={data}
          style={theme}
          collapsed={defaultExpandDepth}
          enableClipboard={false}
          displayDataTypes={showDataTypes}
          displayObjectSize={showObjectSize}
        >
          <JsonView.Arrow>
            <TriangleArrow />
          </JsonView.Arrow>
        </JsonView>
      </div>
    </div>
  );
}
