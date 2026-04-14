import "./globals.css";
import type { Metadata } from "next";
import { ReactNode } from "react";

const siteUrl = "https://apiprism.ai";
const description = "Every API deserves to be understood — by humans, by agents, by machines.";

export const metadata: Metadata = {
  title: {
    default: "APIPrism — API Spectrum for Humans, Agents & Machines",
    template: "%s | APIPrism",
  },
  description,
  metadataBase: new URL(siteUrl),
  openGraph: {
    type: "website",
    locale: "zh_CN",
    url: siteUrl,
    siteName: "APIPrism",
    title: "APIPrism — 一份 API，折射完整光谱",
    description,
  },
  twitter: {
    card: "summary_large_image",
    title: "APIPrism — 一份 API，折射完整光谱",
    description,
  },
  alternates: { canonical: siteUrl },
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
