type Variant = "schema" | "example";

const SchemaIcon = () => (
  <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <path d="M3 9h18M3 15h18M9 9v9M15 9v9" />
  </svg>
);

const CodeIcon = () => (
  <svg className="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="M7 8L3 12l4 4M17 8l4 4-4 4M14 4l-4 16" />
  </svg>
);

export function EmptyPanel({
  variant = "schema",
  message,
}: {
  variant?: Variant;
  message: string;
}) {
  const Icon = variant === "example" ? CodeIcon : SchemaIcon;

  return (
    <div className="flex flex-col items-center gap-3 px-5 py-12 text-center">
      <div className="rounded-xl bg-v-gray-50 p-2.5 text-v-gray-300">
        <Icon />
      </div>
      <p className="text-[13px] text-v-gray-400">{message}</p>
    </div>
  );
}
