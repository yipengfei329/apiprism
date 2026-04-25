"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import type { RevisionSummary } from "../lib/api";

interface RevisionSwitcherProps {
  service: string;
  environment: string;
  revisions: RevisionSummary[];
  viewingRevisionId: string | null;
}

function formatRelative(iso: string) {
  const then = new Date(iso).getTime();
  const diff = Date.now() - then;
  const sec = Math.floor(diff / 1000);
  if (sec < 60) return `${sec} 秒前`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min} 分钟前`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour} 小时前`;
  const day = Math.floor(hour / 24);
  if (day < 30) return `${day} 天前`;
  const month = Math.floor(day / 30);
  if (month < 12) return `${month} 个月前`;
  return new Date(iso).toLocaleDateString("zh-CN");
}

export function RevisionSwitcher({
  service,
  environment,
  revisions,
  viewingRevisionId,
}: RevisionSwitcherProps) {
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return;
    const onClick = (event: MouseEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, [open]);

  if (revisions.length === 0) return null;

  const active = revisions.find((r) =>
    viewingRevisionId ? r.id === viewingRevisionId : r.current
  );
  const viewingOlder = Boolean(viewingRevisionId);

  const baseHref = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}`;

  return (
    <div ref={containerRef} className="relative">
      <button
        onClick={() => setOpen((v) => !v)}
        className="inline-flex items-center gap-1.5 rounded-md bg-[var(--bg-subtle)] px-2.5 py-0.5 font-mono text-[12px] font-medium text-[var(--text-secondary)] transition-colors hover:bg-[var(--accent-bg)] hover:text-[var(--accent)]"
        aria-haspopup="listbox"
        aria-expanded={open}
      >
        <svg className="h-3 w-3" viewBox="0 0 16 16" fill="currentColor">
          <path d="M1.5 8a6.5 6.5 0 1 1 13 0 6.5 6.5 0 0 1-13 0zM8 3a.75.75 0 0 0-.75.75v4.5c0 .2.08.39.22.53l3 3a.75.75 0 0 0 1.06-1.06L8.75 7.94V3.75A.75.75 0 0 0 8 3z" />
        </svg>
        {active ? `#${active.seq}` : "版本"}
        {viewingOlder && (
          <span className="rounded bg-[var(--env-staging-bg)] px-1 text-[10px] text-[var(--env-staging-text)]">
            历史
          </span>
        )}
        <svg className="h-3 w-3 text-[var(--text-tertiary)]" viewBox="0 0 12 12" fill="none">
          <path
            d="M3 4.5L6 7.5L9 4.5"
            stroke="currentColor"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      </button>

      {open && (
        <div
          role="listbox"
          className="absolute right-0 z-20 mt-2 max-h-[320px] w-[300px] overflow-y-auto rounded-xl border border-[var(--border-default)] bg-[var(--bg-surface)] py-1 shadow-lg"
        >
          {revisions.map((rev) => {
            const href = rev.current
              ? baseHref
              : `${baseHref}?revision=${encodeURIComponent(rev.id)}`;
            const isSelected = active?.id === rev.id;
            return (
              <Link
                key={rev.id}
                href={href}
                role="option"
                aria-selected={isSelected}
                onClick={() => setOpen(false)}
                className={`relative flex flex-col gap-0.5 px-3 py-2.5 transition-colors hover:bg-[var(--bg-subtle)] ${isSelected ? "bg-[var(--bg-subtle)]" : ""}`}
              >
                {isSelected && (
                  <span className="absolute inset-y-2 left-0 w-[2px] rounded-r-full bg-[var(--accent)]" />
                )}
                {/* 第一行：序号 + 当前徽章 + 时间 */}
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1.5">
                    <span className="font-mono text-[13px] font-semibold text-[var(--text-primary)]">
                      #{rev.seq}
                    </span>
                    {rev.current && (
                      <span className="rounded bg-[var(--env-prod-bg)] px-1.5 py-[1px] font-mono text-[10px] font-medium text-[var(--env-prod-text)]">
                        当前
                      </span>
                    )}
                  </div>
                  <span className="font-mono text-[11px] text-[var(--text-tertiary)]">
                    {formatRelative(rev.registeredAt)}
                  </span>
                </div>
                {/* 第二行：接口数 + diff + hash */}
                <div className="flex items-center gap-2 text-[11px] text-[var(--text-tertiary)]">
                  {rev.endpointCount != null && (
                    <span>{rev.endpointCount} 接口</span>
                  )}
                  {(rev.addedCount ?? 0) > 0 && (
                    <span className="font-medium text-[var(--color-success,#16a34a)]">+{rev.addedCount}</span>
                  )}
                  {(rev.removedCount ?? 0) > 0 && (
                    <span className="font-medium text-[var(--color-danger,#dc2626)]">-{rev.removedCount}</span>
                  )}
                  {(rev.modifiedCount ?? 0) > 0 && (
                    <span className="font-medium text-[var(--color-warning,#d97706)]">~{rev.modifiedCount}</span>
                  )}
                  <span className="ml-auto font-mono text-[var(--text-quaternary)]">
                    {rev.specHash.substring(0, 7)}
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
