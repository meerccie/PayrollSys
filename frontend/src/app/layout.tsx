import type { Metadata } from "next";
import { Barlow, Inter } from "next/font/google";
import "./globals.css";
import { Providers } from "@/components/providers";

const barlow = Barlow({
  subsets: ["latin"],
  weight: ["500", "600", "700"],
  variable: "--font-display",
  display: "swap",
});

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-sans",
  display: "swap",
});

export const metadata: Metadata = {
  title: "PH Payroll",
  description: "Philippine Payroll Operations & Statutory Dispatch System",
};

export default function RootLayout({ children }: { children: React.ReactNode }): React.ReactElement {
  return (
    <html lang="en" className={`${barlow.variable} ${inter.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
