"use client";

import { useRef, useCallback } from "react";

// 与代码区 text-[13px] leading-[1.75] 一致
const LINE_HEIGHT = 13 * 1.75; // 22.75px

export function CodeAnnotationPanel({
  codeHtml,
  annotations,
  totalLines,
  maxHeight = 400,
}: {
  codeHtml: string;
  annotations: [number, string][];
  totalLines: number;
  maxHeight?: number;
}) {
  const codeRef = useRef<HTMLDivElement>(null);
  const annotRef = useRef<HTMLDivElement>(null);

  // 垂直滚动同步：代码区滚动时，注释面板跟随
  const handleScroll = useCallback(() => {
    if (codeRef.current && annotRef.current) {
      annotRef.current.scrollTop = codeRef.current.scrollTop;
    }
  }, []);

  const annotMap = new Map(annotations);

  return (
    <div className="flex">
      {/* 代码区域 */}
      <div
        ref={codeRef}
        className="min-w-0 flex-1 overflow-auto text-[13px] leading-[1.75]
                   [&_pre]:!bg-transparent [&_pre]:px-4 [&_pre]:py-4
                   [&_code]:font-mono"
        style={{ maxHeight }}
        onScroll={handleScroll}
        dangerouslySetInnerHTML={{ __html: codeHtml }}
      />

      {/* 注释面板 */}
      <div
        ref={annotRef}
        className="w-[220px] shrink-0 overflow-hidden border-l border-white/[0.06] bg-white/[0.02]"
        style={{ maxHeight }}
      >
        <div className="py-4 pl-3 pr-2">
          {Array.from({ length: totalLines }, (_, i) => {
            const text = annotMap.get(i);
            return (
              <div
                key={i}
                className="flex items-center text-[11px] text-[#7f848e]"
                style={{ height: LINE_HEIGHT }}
              >
                {text && (
                  <span className="truncate" title={text}>
                    <span className="mr-1 font-mono text-[#5c6370]">{"//"}</span>
                    {text}
                  </span>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
