import "./globals.css";
import type { Metadata } from "next";
import { ReactNode } from "react";
import { ThemeProvider } from "./components/ThemeProvider";

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

// 预水合脚本：在 React 挂载前同步读取存储的主题并写入 <html class>，避免首屏闪烁。
// next-themes 内部也会做同样的事，但手动注入可以确保即使 Provider 尚未生效也无 FOUC。
const themeInitScript = `(() => {
  try {
    var stored = localStorage.getItem("apiprism-theme");
    var theme = stored;
    if (!theme || theme === "system") {
      theme = window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light";
    }
    var cl = document.documentElement.classList;
    cl.remove("light", "dark");
    cl.add(theme);
    document.documentElement.style.colorScheme = theme;
  } catch (e) {}
})();`;

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body>
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}
