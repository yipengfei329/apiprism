"use client";

import { useEffect, useRef, useState, useCallback } from "react";

export type SectionItem = { id: string; label: string };

// 向上遍历 DOM 找到最近的可滚动祖先
function findScrollParent(el: HTMLElement | null): HTMLElement | null {
  let current = el?.parentElement;
  while (current) {
    const style = getComputedStyle(current);
    if (style.overflowY === "auto" || style.overflowY === "scroll") {
      return current;
    }
    current = current.parentElement;
  }
  return null;
}

export function OnThisPage({ sections }: { sections: SectionItem[] }) {
  const selfRef = useRef<HTMLElement>(null);
  const scrollContainerRef = useRef<HTMLElement | null>(null);
  const [activeId, setActiveId] = useState(sections[0]?.id ?? "");

  // 用 callback ref 绑定后立即查找滚动容器，不触发额外渲染
  const navRefCallback = useCallback((node: HTMLElement | null) => {
    (selfRef as React.MutableRefObject<HTMLElement | null>).current = node;
    scrollContainerRef.current = findScrollParent(node);
  }, []);

  // IntersectionObserver 监听各分区可见性
  useEffect(() => {
    const scrollContainer = scrollContainerRef.current;
    if (!scrollContainer || sections.length === 0) return;

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setActiveId(entry.target.id);
          }
        }
      },
      {
        root: scrollContainer,
        rootMargin: "-10% 0px -70% 0px",
      },
    );

    for (const s of sections) {
      const el = document.getElementById(s.id);
      if (el) observer.observe(el);
    }

    return () => observer.disconnect();
  }, [sections]);

  // 点击锚点平滑滚动
  const handleClick = (id: string) => {
    const el = document.getElementById(id);
    if (!el) return;
    el.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  if (sections.length === 0) return null;

  return (
    <nav ref={navRefCallback} className="sticky top-20">
      <h4 className="mb-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
        页内导航
      </h4>
      <ul className="space-y-1">
        {sections.map((s) => (
          <li key={s.id}>
            <button
              onClick={() => handleClick(s.id)}
              className={`block w-full cursor-pointer border-l-2 py-1 pl-3 text-left text-[13px] transition-colors ${
                activeId === s.id
                  ? "border-[var(--accent)] font-semibold text-[var(--accent)]"
                  : "border-transparent text-[var(--text-tertiary)] hover:text-[var(--text-primary)]"
              }`}
            >
              {s.label}
            </button>
          </li>
        ))}
      </ul>
    </nav>
  );
}
