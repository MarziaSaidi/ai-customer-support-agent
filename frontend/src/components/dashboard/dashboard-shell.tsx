"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ThemeToggle } from "@/components/theme/theme-toggle";
import { useAuth } from "@/contexts/auth-context";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Overview" },
  { href: "/dashboard/analytics", label: "Analytics" },
  { href: "/dashboard/conversations", label: "Conversations" },
  { href: "/dashboard/tickets", label: "Tickets" },
  { href: "/dashboard/documents", label: "Documents" },
  { href: "/dashboard/team", label: "Team" },
  { href: "/dashboard/settings", label: "Settings" },
];

const WIDGET_ITEM = { href: "/widget", label: "Chat widget" };

export function DashboardShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const { user, profile, logout } = useAuth();

  if (!user) return null;

  return (
    <div className="flex min-h-full flex-1 flex-col">
      <header className="sticky top-0 z-30 border-b bg-background/85 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6 sm:py-4">
          <Link
            href="/dashboard"
            className="flex items-center gap-2 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <span
              aria-hidden="true"
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground"
            >
              IQ
            </span>
            <span className="text-lg font-semibold tracking-tight">SupportIQ</span>
          </Link>

          <div className="flex items-center gap-2 sm:gap-3">
            <ThemeToggle />
            <div className="hidden text-right sm:block">
              <p className="text-sm leading-tight font-medium">
                {user.firstName} {user.lastName}
              </p>
              {profile?.companyName && (
                <p className="text-xs leading-tight text-muted-foreground">{profile.companyName}</p>
              )}
            </div>
            <Badge variant="secondary" className="hidden sm:inline-flex">
              {user.role}
            </Badge>
            <Button variant="outline" size="sm" onClick={logout}>
              Log out
            </Button>
          </div>
        </div>

        {/* Below md the sidebar is hidden, so the same destinations live here as
            a scrollable strip — without it the dashboard has no navigation at all
            on a phone. */}
        <nav
          aria-label="Sections"
          className="mx-auto max-w-6xl overflow-x-auto px-4 pb-2 sm:px-6 md:hidden"
        >
          <ul className="flex w-max items-center gap-1">
            {[...navItems, WIDGET_ITEM].map((item) => (
              <li key={item.href}>
                <NavLink item={item} active={pathname === item.href} className="whitespace-nowrap" />
              </li>
            ))}
          </ul>
        </nav>
      </header>

      <div className="mx-auto flex w-full max-w-6xl flex-1 gap-8 px-4 py-8 sm:px-6">
        <aside className="hidden w-48 shrink-0 md:block">
          <nav aria-label="Sections" className="sticky top-24">
            <ul className="space-y-1">
              {navItems.map((item) => (
                <li key={item.href}>
                  <NavLink item={item} active={pathname === item.href} className="block" />
                </li>
              ))}
            </ul>
            <hr className="my-3 border-border" />
            <NavLink
              item={WIDGET_ITEM}
              active={pathname === WIDGET_ITEM.href}
              className="block"
            />
          </nav>
        </aside>

        <main className="min-w-0 flex-1 space-y-8">{children}</main>
      </div>
    </div>
  );
}

function NavLink({
  item,
  active,
  className,
}: {
  item: { href: string; label: string };
  active: boolean;
  className?: string;
}) {
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "rounded-md px-3 py-2 text-sm font-medium transition-colors outline-none",
        "focus-visible:ring-3 focus-visible:ring-ring/50",
        active
          ? "bg-primary text-primary-foreground"
          : "text-muted-foreground hover:bg-muted hover:text-foreground",
        className
      )}
    >
      {item.label}
    </Link>
  );
}
