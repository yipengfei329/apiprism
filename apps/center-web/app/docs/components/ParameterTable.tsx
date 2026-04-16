import { CanonicalParameter } from "../lib/api";
import { HtmlText } from "./HtmlText";
import { SchemaTable } from "./SchemaTable";
import { schemaTypeLabel } from "./schemaUtils";

const LOCATION_ORDER = ["path", "query", "header", "cookie"] as const;

const LOCATION_LABELS: Record<string, string> = {
  path: "Path Parameters",
  query: "Query Parameters",
  header: "Headers",
  cookie: "Cookies",
};

function hasNestedSchema(parameter: CanonicalParameter): boolean {
  const schema = parameter.schema;
  if (!schema || schema.$circular) return false;

  if (schema.properties && Object.keys(schema.properties).length > 0) {
    return true;
  }

  if (schema.type === "array" && schema.items?.properties) {
    return Object.keys(schema.items.properties).length > 0;
  }

  return false;
}

function groupParameters(parameters: CanonicalParameter[]): Array<{
  location: string;
  label: string;
  parameters: CanonicalParameter[];
}> {
  const grouped = new Map<string, CanonicalParameter[]>();

  for (const parameter of parameters) {
    const location = parameter.location || "other";
    const current = grouped.get(location) ?? [];
    current.push(parameter);
    grouped.set(location, current);
  }

  const sortedLocations = [
    ...LOCATION_ORDER.filter((location) => grouped.has(location)),
    ...Array.from(grouped.keys())
      .filter((location) => !LOCATION_ORDER.includes(location as (typeof LOCATION_ORDER)[number]))
      .sort((a, b) => a.localeCompare(b)),
  ];

  return sortedLocations.map((location) => ({
    location,
    label: LOCATION_LABELS[location] ?? `${location[0]?.toUpperCase() ?? ""}${location.slice(1)} Parameters`,
    parameters: (grouped.get(location) ?? []).sort((a, b) => {
      if (a.required !== b.required) {
        return a.required ? -1 : 1;
      }
      return a.name.localeCompare(b.name);
    }),
  }));
}

function ParameterMeta({ parameter }: { parameter: CanonicalParameter }) {
  const schema = parameter.schema;
  const enumValues = schema?.enum ?? [];
  const hasExample = schema?.example !== undefined && schema?.example !== null;

  if (enumValues.length === 0 && !hasExample) return null;

  return (
    <div className="mt-2 flex flex-wrap gap-1.5">
      {enumValues.length > 0 && (
        <span className="inline-flex flex-wrap items-center gap-1 rounded-full bg-[#F5F6F8] px-2.5 py-1 text-[11px] text-v-gray-500">
          <span className="font-medium text-v-gray-400">Enum</span>
          <span className="font-mono">{enumValues.map((value) => String(value)).join(" | ")}</span>
        </span>
      )}
      {hasExample && (
        <span className="inline-flex items-center gap-1 rounded-full bg-[#EEF4FF] px-2.5 py-1 text-[11px] text-v-link">
          <span className="font-medium text-v-gray-400">Example</span>
          <code className="font-mono">{String(schema?.example)}</code>
        </span>
      )}
    </div>
  );
}

function ParameterRow({ parameter }: { parameter: CanonicalParameter }) {
  const nested = hasNestedSchema(parameter);

  return (
    <tr className="border-t border-[#F0F0F3] align-top">
      <td className="px-4 py-4 md:px-5">
        <div className="flex flex-wrap items-center gap-2">
          <code className="font-mono text-[13px] font-semibold text-v-black">
            {parameter.name}
          </code>
          {nested && (
            <span className="rounded-full bg-[#F5F6F8] px-2 py-0.5 text-[10px] font-medium uppercase tracking-[0.08em] text-v-gray-400">
              object
            </span>
          )}
        </div>
      </td>
      <td className="px-4 py-4 md:px-5">
        <code className="rounded-md bg-[#EEF4FF] px-1.5 py-[2px] font-mono text-[11px] text-v-link">
          {schemaTypeLabel(parameter.schema)}
        </code>
      </td>
      <td className="px-4 py-4 md:px-5">
        <span
          className={`font-mono text-[11px] font-semibold ${
            parameter.required ? "text-[#E5484D]" : "text-v-gray-400"
          }`}
        >
          {parameter.required ? "必填" : "选填"}
        </span>
      </td>
      <td className="px-4 py-4 md:px-5">
        {parameter.description ? (
          <HtmlText
            as="div"
            text={parameter.description}
            className="text-[13px] leading-[1.7] text-v-gray-600 [&>p]:mt-1 [&>p:first-child]:mt-0"
          />
        ) : (
          <span className="text-[13px] text-v-gray-400">暂无描述</span>
        )}
        <ParameterMeta parameter={parameter} />
        {nested && parameter.schema && (
          <details className="mt-3 overflow-hidden rounded-xl border border-[#E8E8EC] bg-v-gray-50/40">
            <summary className="cursor-pointer list-none px-4 py-3 text-[12px] font-medium text-v-link marker:hidden">
              查看字段结构
            </summary>
            <div className="border-t border-[#E8E8EC] px-3 pb-2">
              <SchemaTable schema={parameter.schema} />
            </div>
          </details>
        )}
      </td>
    </tr>
  );
}

function ParameterGroup({
  label,
  location,
  parameters,
}: {
  label: string;
  location: string;
  parameters: CanonicalParameter[];
}) {
  const requiredCount = parameters.filter((parameter) => parameter.required).length;

  return (
    <section className="overflow-hidden rounded-2xl border border-[#E8E8EC] bg-white">
      <header className="flex flex-wrap items-center justify-between gap-3 border-b border-[#F0F0F3] bg-v-gray-50/50 px-5 py-4">
        <div className="flex items-center gap-3">
          <h3 className="text-[14px] font-semibold text-v-black">{label}</h3>
          <code className="rounded-full bg-white px-2.5 py-1 font-mono text-[10px] uppercase tracking-[0.08em] text-v-gray-400">
            {location}
          </code>
        </div>
        <div className="flex items-center gap-2 text-[12px] text-v-gray-400">
          <span>{parameters.length} 个参数</span>
          <span className="h-1 w-1 rounded-full bg-[#D0D3DA]" />
          <span>{requiredCount} 个必填</span>
        </div>
      </header>

      <div className="overflow-x-auto">
        <table className="min-w-full table-fixed">
          <colgroup>
            <col className="w-[20%]" />
            <col className="w-[18%]" />
            <col className="w-[12%]" />
            <col className="w-[50%]" />
          </colgroup>
          <thead>
            <tr className="text-left">
              <th className="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-v-gray-400 md:px-5">Name</th>
              <th className="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-v-gray-400 md:px-5">Type</th>
              <th className="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-v-gray-400 md:px-5">Required</th>
              <th className="px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.08em] text-v-gray-400 md:px-5">Description</th>
            </tr>
          </thead>
          <tbody>
            {parameters.map((parameter) => (
              <ParameterRow
                key={`${parameter.location}:${parameter.name}`}
                parameter={parameter}
              />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export function ParameterTable({ parameters }: { parameters: CanonicalParameter[] }) {
  if (parameters.length === 0) return null;

  const groups = groupParameters(parameters);

  return (
    <div className="space-y-5">
      {groups.map((group) => (
        <ParameterGroup
          key={group.location}
          label={group.label}
          location={group.location}
          parameters={group.parameters}
        />
      ))}
    </div>
  );
}
