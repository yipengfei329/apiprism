"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { DeleteServiceDialog } from "../../components/DeleteServiceDialog";

interface ServiceActionsProps {
  service: string;
  environment: string;
}

export function ServiceActions({ service, environment }: ServiceActionsProps) {
  const router = useRouter();
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <>
      <button
        onClick={() => setDialogOpen(true)}
        className="flex w-full items-center justify-center gap-2 rounded-xl border border-[var(--danger-border)] bg-[var(--danger-bg)] px-4 py-3.5 text-[14px] font-medium text-[var(--danger)] transition-colors hover:opacity-80"
      >
        <svg className="h-4 w-4" viewBox="0 0 16 16" fill="currentColor">
          <path d="M6.5 1.75a.25.25 0 01.25-.25h2.5a.25.25 0 01.25.25V3h-3V1.75zm4.5 0V3h2.25a.75.75 0 010 1.5H2.75a.75.75 0 010-1.5H5V1.75C5 .784 5.784 0 6.75 0h2.5C10.216 0 11 .784 11 1.75zM4.496 6.675a.75.75 0 10-1.492.15l.66 6.6A1.75 1.75 0 005.405 15h5.19c.9 0 1.652-.681 1.741-1.576l.66-6.6a.75.75 0 00-1.492-.149l-.66 6.6a.25.25 0 01-.249.225h-5.19a.25.25 0 01-.249-.225l-.66-6.6z" />
        </svg>
        删除环境
      </button>
      <DeleteServiceDialog
        service={service}
        environment={environment}
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        onDeleted={() => router.push("/")}
      />
    </>
  );
}
