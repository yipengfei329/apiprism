"use client";

import { useMemo } from "react";
import { JsonPreview } from "@/app/components/JsonPreview";
import type { JsonSchema } from "../lib/api";
import { generateExample } from "./generateExample";

export function ExampleViewer({ schema }: { schema: JsonSchema }) {
  const example = useMemo(() => generateExample(schema), [schema]);

  if (example === null || example === undefined) return null;

  // JsonPreview 要求 data 为 object 或 array
  const data = typeof example === "object" ? example : { value: example };

  return (
    <div className="mt-4">
      <div className="mb-2.5 text-[11px] font-semibold uppercase tracking-[0.08em] text-v-gray-400">
        示例
      </div>
      <JsonPreview data={data as object} maxHeight={400} />
    </div>
  );
}
