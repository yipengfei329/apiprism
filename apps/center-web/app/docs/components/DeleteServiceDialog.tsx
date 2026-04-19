"use client";

import { useEffect, useRef, useState } from "react";
import { getPublicApiUrl } from "@/app/lib/public-api";

interface DeleteServiceDialogProps {
  service: string;
  environment: string;
  open: boolean;
  onClose: () => void;
  onDeleted: () => void;
}

export function DeleteServiceDialog({
  service,
  environment,
  open,
  onClose,
  onDeleted,
}: DeleteServiceDialogProps) {
  const [confirmText, setConfirmText] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (open) {
      setConfirmText("");
      setError(null);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [open, onClose]);

  async function handleDelete() {
    setDeleting(true);
    setError(null);
    try {
      const res = await fetch(
        getPublicApiUrl(
          `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}`
        ),
        { method: "DELETE" }
      );
      if (res.status === 204) {
        onDeleted();
      } else {
        setError(`删除失败，服务器返回 ${res.status}`);
      }
    } catch {
      setError("网络请求失败，请重试");
    } finally {
      setDeleting(false);
    }
  }

  if (!open) return null;

  const canDelete = confirmText === service && !deleting;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ backgroundColor: "var(--overlay)" }}
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="w-full max-w-[440px] rounded-2xl bg-[var(--bg-surface)] p-6 shadow-2xl">
        {/* 标题行 */}
        <div className="mb-4 flex items-center gap-2.5">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[var(--danger-bg)]">
            <svg className="h-4 w-4 text-[var(--danger)]" viewBox="0 0 20 20" fill="currentColor">
              <path fillRule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495zM10 5a.75.75 0 01.75.75v3.5a.75.75 0 01-1.5 0v-3.5A.75.75 0 0110 5zm0 9a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
            </svg>
          </span>
          <h2 className="text-[15px] font-semibold text-[var(--text-primary)]">删除环境</h2>
        </div>

        {/* 说明 */}
        <p className="mb-3 text-[13px] leading-[1.6] text-[var(--text-secondary)]">
          此操作将永久删除服务{" "}
          <code className="rounded bg-[var(--bg-subtle)] px-1.5 py-0.5 font-mono text-[12px] text-[var(--text-primary)]">
            {service}
          </code>{" "}
          在{" "}
          <code className="rounded bg-[var(--bg-subtle)] px-1.5 py-0.5 font-mono text-[12px] text-[var(--text-primary)]">
            {environment}
          </code>{" "}
          环境下的文档及 MCP 配置，<strong className="text-[var(--text-primary)] font-medium">无法撤销</strong>。
        </p>
        <p className="mb-5 text-[13px] leading-[1.6] text-[var(--text-secondary)]">
          若该服务存在其他环境，其他环境的文档不受影响。
        </p>

        {/* 确认输入 */}
        <label className="mb-1.5 block text-[12px] font-medium text-[var(--text-primary)]">
          请输入服务名称以确认：
        </label>
        <input
          ref={inputRef}
          type="text"
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          onKeyDown={(e) => { if (e.key === "Enter" && canDelete) handleDelete(); }}
          placeholder={service}
          disabled={deleting}
          className="mb-4 w-full rounded-lg border border-[var(--border-default)] bg-[var(--bg-surface)] px-3 py-2 font-mono text-[13px] text-[var(--text-primary)] outline-none placeholder:text-[var(--text-quaternary)] focus:border-[var(--danger)] focus:ring-2 focus:ring-[var(--danger)]/20 disabled:opacity-50"
        />

        {/* 错误提示 */}
        {error && (
          <p className="mb-3 text-[12px] text-[var(--danger)]">{error}</p>
        )}

        {/* 操作按钮 */}
        <div className="flex gap-2.5">
          <button
            onClick={onClose}
            disabled={deleting}
            className="flex-1 rounded-xl border border-[var(--border-default)] bg-[var(--bg-surface)] py-2 text-[13px] font-medium text-[var(--text-secondary)] transition-colors hover:bg-[var(--bg-subtle)] disabled:opacity-50"
          >
            取消
          </button>
          <button
            onClick={handleDelete}
            disabled={!canDelete}
            className="flex-1 rounded-xl bg-[var(--danger)] py-2 text-[13px] font-medium text-white transition-colors hover:opacity-90 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {deleting ? "删除中…" : "删除环境"}
          </button>
        </div>
      </div>
    </div>
  );
}
