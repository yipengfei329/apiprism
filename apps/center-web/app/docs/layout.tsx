import { ReactNode } from "react";
import { getServices } from "./lib/api";
import { SidebarLayout } from "./components/SidebarLayout";

export default async function DocsLayout({ children }: { children: ReactNode }) {
  const services = await getServices();

  return (
    <SidebarLayout services={services}>
      {children}
    </SidebarLayout>
  );
}
