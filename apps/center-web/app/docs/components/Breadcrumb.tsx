import Link from "next/link";
import {
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
  return (
    <nav aria-label="Breadcrumb">
      <ol className="flex items-center gap-1 text-[13px]">
        {items.map((item, i) => {
          const isLast = i === items.length - 1;
          const Icon = item.icon ? iconMap[item.icon] : null;

          return (
            <li key={i} className="flex items-center gap-1">
              {/* 分隔符 */}
              {i > 0 && (
                <CaretRight
                  weight="bold"
                  className="mx-0.5 h-3 w-3 shrink-0 text-v-gray-400/60"
                />
              )}

              {/* 面包屑项 */}
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
                <span className="flex items-center gap-1.5 rounded-md px-1.5 py-0.5 font-semibold text-v-black">
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
