"use client";

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { EmptyState, ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { MetricGroup } from "@/components/dashboard/metrics";
import { useAuth } from "@/contexts/auth-context";
import { api } from "@/lib/api";
import { useAsync } from "@/lib/use-async";

function formatResponseTime(ms: number) {
  if (ms <= 0) return "—";
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(1)} s`;
}

function formatDay(date: string) {
  const parsed = new Date(date + "T00:00:00Z");
  return parsed.toLocaleDateString(undefined, { weekday: "short", day: "numeric" });
}

/**
 * Values are rendered as text above each bar, so the chart is readable without
 * hovering and needs no tooltip to be understood. The bar itself animates with
 * transform only.
 */
function BarChart({ items }: { items: { label: string; value: number }[] }) {
  const max = Math.max(1, ...items.map((item) => item.value));

  return (
    <div className="flex h-48 items-end gap-2">
      {items.map((item, index) => (
        <div key={item.label} className="flex min-w-0 flex-1 flex-col items-center gap-1.5">
          <span className="text-xs font-medium tabular-nums">{item.value}</span>
          <div className="flex w-full flex-1 items-end">
            <div
              className="animate-bar w-full rounded-t-sm bg-foreground/80"
              style={{
                height: `${Math.max(2, (item.value / max) * 100)}%`,
                animationDelay: `${index * 40}ms`,
              }}
            />
          </div>
          <span className="w-full truncate text-center text-[11px] text-muted-foreground">
            {item.label}
          </span>
        </div>
      ))}
    </div>
  );
}

export default function AnalyticsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const { data, status, error, slow, reload } = useAsync(
    companyId ? () => api.getAnalytics(companyId) : null,
    [companyId]
  );

  const loading = status === "loading";

  const ticketBreakdown = data
    ? [
        { label: "Open", value: data.ticketStatusBreakdown.open },
        { label: "In progress", value: data.ticketStatusBreakdown.inProgress },
        { label: "Resolved", value: data.ticketStatusBreakdown.resolved },
        { label: "Closed", value: data.ticketStatusBreakdown.closed },
      ]
    : [];

  const trendItems =
    data?.conversationTrend.map((point) => ({
      label: formatDay(point.date),
      value: point.count,
    })) ?? [];

  if (status === "error") {
    return (
      <>
        <PageHeader
          title="Analytics"
          description="Track conversations, resolution rate, response time, and common questions."
        />
        <ErrorState
          title="Couldn't load analytics"
          description={error}
          onRetry={reload}
        />
      </>
    );
  }

  return (
    <>
      <PageHeader
        title="Analytics"
        description="Track conversations, resolution rate, response time, and common questions."
      />

      <section aria-label="Key metrics" className="space-y-4">
        <MetricGroup
          loading={loading}
          metrics={[
            { label: "Conversations", value: data?.totalConversations ?? null },
            {
              label: "Resolved",
              value: data?.resolvedConversations ?? null,
              hint: data ? `${data.aiResolutionRate.toFixed(1)}% of all chats` : undefined,
            },
            {
              label: "Avg response",
              value: data ? formatResponseTime(data.averageResponseTimeMs) : null,
            },
            {
              label: "Satisfaction",
              value: data ? data.customerSatisfaction.toFixed(1) : null,
              hint: data ? "out of 5" : undefined,
            },
          ]}
        />
        {loading && slow && <LoadingNotice label="Waking the server" slow />}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Conversations</CardTitle>
            <CardDescription>New chats started per day, last 7 days</CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <SkeletonRows rows={3} />
            ) : trendItems.length === 0 ? (
              <EmptyState
                title="No conversations in the last 7 days"
                description="The trend fills in as customers start chats."
              />
            ) : (
              <BarChart items={trendItems} />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ticket status</CardTitle>
            <CardDescription>Current ticket queue breakdown</CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <SkeletonRows rows={3} />
            ) : ticketBreakdown.every((item) => item.value === 0) ? (
              <EmptyState
                title="No tickets yet"
                description="Tickets created by agents or the AI appear here."
              />
            ) : (
              <BarChart items={ticketBreakdown} />
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Top customer questions</CardTitle>
          <CardDescription>Most frequent questions from the chat widget</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <SkeletonRows rows={4} />
          ) : !data || data.topQuestions.length === 0 ? (
            <EmptyState
              title="No questions recorded yet"
              description="Questions asked through the widget are grouped here."
            />
          ) : (
            <ol className="divide-y">
              {data.topQuestions.map((item, index) => (
                <li
                  key={`${item.question}-${index}`}
                  className="flex items-baseline gap-4 py-2.5 text-sm first:pt-0"
                >
                  <span className="w-5 shrink-0 text-eyebrow text-muted-foreground tabular-nums">
                    {index + 1}
                  </span>
                  <span className="min-w-0 flex-1">{item.question}</span>
                  <span className="shrink-0 text-sm font-medium tabular-nums">
                    {item.count}
                    <span className="text-muted-foreground">×</span>
                  </span>
                </li>
              ))}
            </ol>
          )}
        </CardContent>
      </Card>
    </>
  );
}
