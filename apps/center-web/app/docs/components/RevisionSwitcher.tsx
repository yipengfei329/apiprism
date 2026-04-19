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
  if (sec < 60) return `${sec}s ago`;
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min}m ago`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour}h ago`;
  const day = Math.floor(hour / 24);
  if (day < 30) return `${day}d ago`;
  const month = Math.floor(day / 30);
  if (month < 12) return `${month}mo ago`;
  return new Date(iso).toLocaleDateString();
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
          className="absolute right-0 z-20 mt-2 max-h-[320px] w-[280px] overflow-y-auto rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface)] py-1 shadow-lg"
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
                className="flex items-center justify-between gap-2 px-3 py-2 hover:bg-[var(--bg-subtle)]"
              >
                <div className="flex items-center gap-2">
                  <span className="font-mono text-[12px] font-semibold text-[var(--text-primary)]">
                    #{rev.seq}
                  </span>
                  {rev.current && (
                    <span className="rounded bg-[var(--env-prod-bg)] px-1.5 py-[1px] font-mono text-[10px] font-medium text-[var(--env-prod-text)]">
                      当前
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 text-[11px] text-[var(--text-tertiary)]">
                  <span className="font-mono">{rev.specHash.substring(0, 7)}</span>
                  <span>·</span>
                  <span>{formatRelative(rev.registeredAt)}</span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
