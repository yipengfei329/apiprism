import Link from "next/link";
import {
  CaretLeft,
  CaretRight,
  Cube,
  FolderSimple,
  FileCode,
} from "@phosphor-icons/react/dist/ssr";

export type BreadcrumbItem = {
  label: string;
  href?: string;
  icon?: "service" | "group" | "operation";
};

const iconMap = {
  service: Cube,
  group: FolderSimple,
  operation: FileCode,
} as const;

export function Breadcrumb({ items }: { items: BreadcrumbItem[] }) {
  const lastIndex = items.length - 1;
  const current = items[lastIndex];
  const parent = lastIndex > 0 ? items[lastIndex - 1] : null;
  const CurrentIcon = current.icon ? iconMap[current.icon] : null;

  return (
    <nav aria-label="Breadcrumb" className="min-w-0">
      {/* 移动端：父级返回按钮 + 当前项，避免长面包屑溢出屏幕 */}
      <div className="flex min-w-0 items-center gap-1 text-[13px] sm:hidden">
        {parent && (
          <>
            {parent.href ? (
              <Link
                href={parent.href}
                className="flex shrink-0 items-center gap-1 rounded-md px-1.5 py-0.5 font-medium text-v-gray-500 transition-all duration-150 hover:bg-v-gray-50 hover:text-v-black"
              >
                <CaretLeft weight="bold" className="h-3 w-3" />
                <span className="max-w-[6rem] truncate">{parent.label}</span>
              </Link>
            ) : (
              <span className="flex shrink-0 items-center gap-1 px-1.5 py-0.5 font-medium text-v-gray-500">
                <span className="max-w-[6rem] truncate">{parent.label}</span>
              </span>
            )}
            <CaretRight
              weight="bold"
              className="mx-0.5 h-3 w-3 shrink-0 text-v-gray-400/60"
            />
          </>
        )}
        <span className="flex min-w-0 items-center gap-1.5 rounded-md px-1.5 py-0.5 font-semibold text-v-black">
          {CurrentIcon && (
            <CurrentIcon
              weight="duotone"
              className="h-3.5 w-3.5 shrink-0 text-v-link"
            />
          )}
          <span className="truncate">{current.label}</span>
        </span>
      </div>

      {/* 桌面端：完整层级 */}
      <ol className="hidden min-w-0 items-center gap-1 text-[13px] sm:flex">
        {items.map((item, i) => {
          const isLast = i === lastIndex;
          const Icon = item.icon ? iconMap[item.icon] : null;

          return (
            <li
              key={i}
              className={`flex items-center gap-1 ${
                isLast ? "min-w-0 flex-1" : "shrink-0"
              }`}
            >
              {i > 0 && (
                <CaretRight
                  weight="bold"
                  className="mx-0.5 h-3 w-3 shrink-0 text-v-gray-400/60"
                />
              )}

              {item.href && !isLast ? (
                <Link
                  href={item.href}
                  className="group/crumb flex items-center gap-1.5 rounded-md px-1.5 py-0.5 font-medium text-v-gray-500 transition-all duration-150 hover:bg-v-gray-50 hover:text-v-black"
                >
                  {Icon && (
                    <Icon
                      weight="duotone"
                      className="h-3.5 w-3.5 shrink-0 text-v-gray-400 transition-colors duration-150 group-hover/crumb:text-v-link"
                    />
                  )}
                  <span className="truncate">{item.label}</span>
                </Link>
              ) : (
                <span className="flex min-w-0 items-center gap-1.5 rounded-md px-1.5 py-0.5 font-semibold text-v-black">
                  {Icon && (
                    <Icon
                      weight="duotone"
                      className="h-3.5 w-3.5 shrink-0 text-v-link"
                    />
                  )}
                  <span className="truncate">{item.label}</span>
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
