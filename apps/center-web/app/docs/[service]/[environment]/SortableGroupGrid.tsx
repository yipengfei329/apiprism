"use client";

import { useState, useCallback } from "react";
import Link from "next/link";
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  rectSortingStrategy,
  arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { DotsSixVertical } from "@phosphor-icons/react";
import { getPublicApiUrl } from "@/app/lib/public-api";
import { HtmlText } from "../../components/HtmlText";
import type { CanonicalGroup } from "../../lib/api";

interface SortableGroupGridProps {
  service: string;
  environment: string;
  groups: CanonicalGroup[];
}

function SortableGroupCard({
  group,
  href,
}: {
  group: CanonicalGroup;
  href: string;
}) {
  const {
    attributes,
    listeners,
    setNodeRef,
    setActivatorNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: group.slug });

  return (
    <div
      ref={setNodeRef}
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
        opacity: isDragging ? 0.5 : 1,
        position: "relative",
      }}
      className="group/card"
    >
      {/* 拖拽手柄：悬停时显示 */}
      <button
        ref={setActivatorNodeRef}
        {...attributes}
        {...listeners}
        aria-label={`拖拽排序: ${group.name}`}
        className="absolute left-2 top-1/2 z-10 -translate-y-1/2 cursor-grab rounded p-1 opacity-0 transition-opacity duration-150 group-hover/card:opacity-60 active:cursor-grabbing text-[var(--text-tertiary)] hover:opacity-100"
      >
        <DotsSixVertical size={16} weight="bold" />
      </button>

      <Link
        href={href}
        className="group block rounded-xl bg-[var(--bg-surface)] p-5 pl-9 transition-all v-card-full v-card-full-hover"
      >
        <h3
          className="font-semibold text-[var(--text-primary)]"
          style={{ letterSpacing: "-0.015em" }}
        >
          {group.name}
        </h3>
        {group.description && (
          <HtmlText
            as="p"
            text={group.description}
            className="mt-1.5 line-clamp-2 text-[13px] leading-[1.65] text-[var(--text-secondary)]"
          />
        )}
        <div className="mt-4 flex items-center justify-between">
          <p className="font-mono text-[12px] text-[var(--text-tertiary)]">
            {group.operations.length} 个接口
          </p>
          <svg
            className="h-3.5 w-3.5 text-[var(--text-tertiary)] opacity-0 transition-all duration-200 group-hover:translate-x-0.5 group-hover:opacity-100"
            viewBox="0 0 14 14"
            fill="none"
          >
            <path
              d="M3 7h8M7.5 3.5L11 7l-3.5 3.5"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </div>
      </Link>
    </div>
  );
}

export function SortableGroupGrid({
  service,
  environment,
  groups: initialGroups,
}: SortableGroupGridProps) {
  const [groups, setGroups] = useState(initialGroups);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      // 5px 拖拽阈值，防止与卡片点击冲突
      activationConstraint: { distance: 5 },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const persistOrder = useCallback(
    (slugs: string[]) => {
      fetch(
        getPublicApiUrl(
          `/api/v1/services/${encodeURIComponent(service)}/env/${encodeURIComponent(environment)}/tag-order`
        ),
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ slugs }),
        }
      ).catch(() => {
        // 持久化失败静默处理；下次刷新页面会恢复服务端保存的最新顺序
      });
    },
    [service, environment]
  );

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      const { active, over } = event;
      if (!over || active.id === over.id) return;

      setGroups((prev) => {
        const oldIndex = prev.findIndex((g) => g.slug === active.id);
        const newIndex = prev.findIndex((g) => g.slug === over.id);
        const reordered = arrayMove(prev, oldIndex, newIndex);
        persistOrder(reordered.map((g) => g.slug));
        return reordered;
      });
    },
    [persistOrder]
  );

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragEnd={handleDragEnd}
    >
      <SortableContext
        items={groups.map((g) => g.slug)}
        strategy={rectSortingStrategy}
      >
        <div className="grid gap-3 sm:grid-cols-2">
          {groups.map((group) => {
            const href = `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(group.slug)}`;
            return (
              <SortableGroupCard key={group.slug} group={group} href={href} />
            );
          })}
        </div>
      </SortableContext>
    </DndContext>
  );
}
