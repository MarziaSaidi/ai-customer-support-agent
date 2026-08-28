import Link from "next/link";

import { Card, CardContent, CardDescription, CardHeader } from "@/components/ui/card";

/** Shared frame for log in and register so the two screens stay identical. */
export function AuthShell({
  title,
  description,
  children,
  footer,
}: {
  title: string;
  description: string;
  children: React.ReactNode;
  footer: React.ReactNode;
}) {
  return (
    <div className="flex flex-1 flex-col items-center justify-center px-4 py-12">
      <Link
        href="/"
        className="mb-8 flex items-center gap-2 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
      >
        <span
          aria-hidden="true"
          className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground"
        >
          IQ
        </span>
        <span className="text-lg font-semibold tracking-tight">SupportIQ</span>
      </Link>

      <Card className="animate-enter w-full max-w-md">
        <CardHeader>
          {/* A real h1 — these are top-level pages, not cards inside a page. */}
          <h1 className="font-heading text-lg leading-snug font-medium">{title}</h1>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-6">
          {children}
          <p className="text-center text-sm text-muted-foreground">{footer}</p>
        </CardContent>
      </Card>
    </div>
  );
}
