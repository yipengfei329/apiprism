"use client";

import { useState, useCallback } from "react";
import {
  DndContext,
  DragOverlay,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
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
import type { CanonicalGroup } from "../../lib/api";
import { GroupCard } from "./GroupCard";

interface SortableGroupGridProps {
  service: string;
  environment: string;
  groups: CanonicalGroup[];
}

function buildAgentDocPath(service: string, environment: string, slug: string) {
  return `/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(slug)}/apidocs.md`;
}

function buildGroupHref(service: string, environment: string, slug: string) {
  return `/docs/${encodeURIComponent(service)}/${encodeURIComponent(environment)}/${encodeURIComponent(slug)}`;
}

function SortableGroupCard({
  service,
  environment,
  group,
}: {
  service: string;
  environment: string;
  group: CanonicalGroup;
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
      className="h-full"
      style={{
        transform: CSS.Transform.toString(transform),
        transition,
        // 拖拽中原位置仅保留占位轮廓，悬浮副本由 DragOverlay 渲染
        opacity: isDragging ? 0.35 : 1,
      }}
    >
      <GroupCard
        group={group}
        href={buildGroupHref(service, environment, group.slug)}
        agentDocPath={buildAgentDocPath(service, environment, group.slug)}
        dragHandle={
          <button
            ref={setActivatorNodeRef}
            {...attributes}
            {...listeners}
            type="button"
            aria-label={`拖拽排序: ${group.name}`}
            className="absolute right-2 top-2 z-10 cursor-grab touch-none rounded-md p-1 text-[var(--text-tertiary)] opacity-25 transition-opacity duration-150 hover:bg-[var(--bg-subtle)] hover:opacity-80 active:cursor-grabbing"
          >
            <DotsSixVertical size={16} weight="bold" />
          </button>
        }
      />
    </div>
  );
}

export function SortableGroupGrid({
  service,
  environment,
  groups: initialGroups,
}: SortableGroupGridProps) {
  const [groups, setGroups] = useState(initialGroups);
  const [activeId, setActiveId] = useState<string | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      // 5px 拖拽阈值，防止与卡片点击/Agent 文档按钮冲突
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

  const handleDragStart = useCallback((event: DragStartEvent) => {
    setActiveId(String(event.active.id));
  }, []);

  const handleDragEnd = useCallback(
    (event: DragEndEvent) => {
      setActiveId(null);
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

  const handleDragCancel = useCallback(() => setActiveId(null), []);

  const activeGroup = activeId
    ? groups.find((g) => g.slug === activeId) ?? null
    : null;

  return (
    <DndContext
      id="sortable-group-grid"
      sensors={sensors}
      collisionDetection={closestCenter}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      onDragCancel={handleDragCancel}
    >
      <SortableContext
        items={groups.map((g) => g.slug)}
        strategy={rectSortingStrategy}
      >
        <div className="grid auto-rows-fr gap-3 md:grid-cols-2">
          {groups.map((group) => (
            <SortableGroupCard
              key={group.slug}
              service={service}
              environment={environment}
              group={group}
            />
          ))}
        </div>
      </SortableContext>
      {/* 拖拽悬浮层：让被拖卡片"浮起"跟随光标，体感比单纯透明化原位更顺滑 */}
      <DragOverlay dropAnimation={{ duration: 180, easing: "cubic-bezier(0.18, 0.67, 0.6, 1.22)" }}>
        {activeGroup ? (
          <div className="rotate-[1.5deg] cursor-grabbing shadow-2xl">
            <GroupCard
              group={activeGroup}
              href={buildGroupHref(service, environment, activeGroup.slug)}
              agentDocPath={buildAgentDocPath(service, environment, activeGroup.slug)}
            />
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}
