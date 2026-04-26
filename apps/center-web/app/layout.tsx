import "./globals.css";
import type { Metadata } from "next";
import { ReactNode } from "react";
import { Geist, Geist_Mono } from "next/font/google";
import { ThemeProvider } from "./components/ThemeProvider";

const geistSans = Geist({
  subsets: ["latin"],
  variable: "--font-geist-sans",
  display: "swap",
});

const geistMono = Geist_Mono({
  subsets: ["latin"],
  variable: "--font-geist-mono",
  display: "swap",
});

const siteUrl = "https://apiprism.ai";
const description = "每一个 API，都值得被充分理解 — 无论是人类、AI Agent，还是自动化机器。";

export const metadata: Metadata = {
  title: {
    default: "APIPrism — 一份 API，折射完整光谱",
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
    <html lang="zh-CN" suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className={`${geistSans.variable} ${geistMono.variable}`}>
        <ThemeProvider>{children}</ThemeProvider>
      </body>
    </html>
  );
}
