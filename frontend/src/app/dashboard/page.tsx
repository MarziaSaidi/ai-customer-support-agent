"use client";

import Link from "next/link";
import { ChevronRightIcon } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { EmptyState, ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { MetricGroup } from "@/components/dashboard/metrics";
import { useAuth } from "@/contexts/auth-context";
import { api } from "@/lib/api";
import { useAsync } from "@/lib/use-async";

const QUICK_ACTIONS = [
  { href: "/dashboard/tickets", label: "Support tickets", hint: "Triage, assign, and resolve" },
  { href: "/dashboard/documents", label: "Knowledge base", hint: "Upload the docs the AI answers from" },
  { href: "/dashboard/analytics", label: "Analytics", hint: "Resolution rate and common questions" },
  { href: "/widget", label: "Chat widget", hint: "Preview what customers see" },
  { href: "/dashboard/team", label: "Team", hint: "Manage workspace access" },
  { href: "/dashboard/settings", label: "Settings", hint: "Company profile and AI behavior" },
];

function formatResponseTime(ms: number) {
  if (ms <= 0) return "—";
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(1)} s`;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const analytics = useAsync(
    companyId ? () => api.getAnalytics(companyId) : null,
    [companyId]
  );
  const sessions = useAsync(
    companyId ? () => api.getSessions(companyId) : null,
    [companyId]
  );

  const metricsLoading = analytics.status === "loading";
  const data = analytics.data;

  return (
    <>
      <PageHeader
        title="Overview"
        description="Monitor conversations, tickets, and AI performance."
      />

      <section aria-label="Key metrics" className="space-y-4">
        {analytics.status === "error" ? (
          <ErrorState
            title="Couldn't load your metrics"
            description={analytics.error}
            onRetry={analytics.reload}
          />
        ) : (
          <>
            <MetricGroup
              loading={metricsLoading}
              metrics={[
                { label: "Conversations", value: data?.totalConversations ?? null },
                {
                  label: "AI resolution rate",
                  value: data ? `${data.aiResolutionRate.toFixed(1)}%` : null,
                },
                { label: "Open tickets", value: data?.openTickets ?? null },
                {
                  label: "Avg response",
                  value: data ? formatResponseTime(data.averageResponseTimeMs) : null,
                },
              ]}
            />
            {metricsLoading && analytics.slow && (
              <LoadingNotice label="Waking the server" slow />
            )}
          </>
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Recent conversations</CardTitle>
            <CardDescription>Latest customer chat sessions</CardDescription>
          </CardHeader>
          <CardContent>
            {sessions.status === "loading" && (
              <>
                <SkeletonRows rows={4} />
                <LoadingNotice
                  className="sr-only"
                  label="Loading conversations"
                  slow={sessions.slow}
                />
              </>
            )}

            {sessions.status === "error" && (
              <ErrorState description={sessions.error} onRetry={sessions.reload} />
            )}

            {sessions.status === "ready" &&
              (sessions.data && sessions.data.length > 0 ? (
                <>
                  <ul className="divide-y">
                    {sessions.data.slice(0, 5).map((session) => (
                      <li
                        key={session.id}
                        className="flex items-center justify-between gap-3 py-2.5 text-sm first:pt-0"
                      >
                        <span className="truncate">
                          {session.customerName ||
                            session.customerEmail ||
                            `Conversation #${session.id}`}
                        </span>
                        <Badge variant={session.resolved ? "success" : "secondary"}>
                          {session.status}
                        </Badge>
                      </li>
                    ))}
                  </ul>
                  <Link
                    href="/dashboard/conversations"
                    className="group mt-4 inline-flex items-center gap-1 rounded-md text-sm font-medium text-foreground outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
                  >
                    View all conversations
                    <ChevronRightIcon
                      aria-hidden="true"
                      className="size-4 transition-transform group-hover:translate-x-0.5"
                    />
                  </Link>
                </>
              ) : (
                <EmptyState
                  title="No conversations yet"
                  description="Chats started from your widget will show up here."
                />
              ))}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Jump to</CardTitle>
            <CardDescription>Manage your support platform</CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="divide-y">
              {QUICK_ACTIONS.map((action) => (
                <li key={action.href}>
                  <Link
                    href={action.href}
                    className="group flex items-center justify-between gap-3 rounded-md py-2.5 outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
                  >
                    <span className="min-w-0">
                      <span className="block text-sm font-medium group-hover:underline">
                        {action.label}
                      </span>
                      <span className="block truncate text-xs text-muted-foreground">
                        {action.hint}
                      </span>
                    </span>
                    <ChevronRightIcon
                      aria-hidden="true"
                      className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-0.5"
                    />
                  </Link>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      </div>
    </>
  );
}
