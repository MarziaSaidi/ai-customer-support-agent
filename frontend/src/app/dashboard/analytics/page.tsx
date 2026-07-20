"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/contexts/auth-context";
import { api, type AnalyticsResponse } from "@/lib/api";

function formatResponseTime(ms: number) {
  if (ms <= 0) return "—";
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(1)} s`;
}

function formatDay(date: string) {
  const parsed = new Date(date + "T00:00:00Z");
  return parsed.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric" });
}

function BarChart({
  items,
  labelKey,
  valueKey,
}: {
  items: { [key: string]: string | number }[];
  labelKey: string;
  valueKey: string;
}) {
  const max = Math.max(1, ...items.map((item) => Number(item[valueKey]) || 0));

  return (
    <div className="flex h-48 items-end gap-2">
      {items.map((item, index) => {
        const value = Number(item[valueKey]) || 0;
        const height = `${Math.max(8, (value / max) * 100)}%`;
        return (
          <div key={index} className="flex min-w-0 flex-1 flex-col items-center gap-2">
            <div className="flex h-full w-full items-end">
              <div
                className="w-full rounded-t-md bg-primary/80 transition-all"
                style={{ height }}
                title={`${item[labelKey]}: ${value}`}
              />
            </div>
            <span className="truncate text-[10px] text-muted-foreground">{String(item[labelKey])}</span>
          </div>
        );
      })}
    </div>
  );
}

export default function AnalyticsPage() {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null);

  useEffect(() => {
    if (!user?.companyId) return;
    api.getAnalytics(user.companyId).then(setAnalytics).catch(console.error);
  }, [user?.companyId]);

  const ticketBreakdown = analytics
    ? [
        { label: "Open", value: analytics.ticketStatusBreakdown.open },
        { label: "In progress", value: analytics.ticketStatusBreakdown.inProgress },
        { label: "Resolved", value: analytics.ticketStatusBreakdown.resolved },
        { label: "Closed", value: analytics.ticketStatusBreakdown.closed },
      ]
    : [];

  const trendItems =
    analytics?.conversationTrend.map((point) => ({
      label: formatDay(point.date),
      value: point.count,
    })) ?? [];

  return (
    <>
      <h1 className="text-2xl font-bold">Analytics</h1>
      <p className="mt-1 text-muted-foreground">
        Track conversations, resolution rate, response time, and common questions.
      </p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard title="Conversations" value={analytics?.totalConversations ?? "—"} />
        <MetricCard
          title="Resolved"
          value={
            analytics
              ? `${analytics.resolvedConversations} (${analytics.aiResolutionRate.toFixed(1)}%)`
              : "—"
          }
        />
        <MetricCard title="Avg response" value={analytics ? formatResponseTime(analytics.averageResponseTimeMs) : "—"} />
        <MetricCard
          title="Satisfaction"
          value={analytics ? analytics.customerSatisfaction.toFixed(1) : "—"}
        />
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Conversations (7 days)</CardTitle>
            <CardDescription>New chats started per day</CardDescription>
          </CardHeader>
          <CardContent>
            {trendItems.length === 0 ? (
              <p className="text-sm text-muted-foreground">No data yet.</p>
            ) : (
              <BarChart items={trendItems} labelKey="label" valueKey="value" />
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Ticket status</CardTitle>
            <CardDescription>Current ticket queue breakdown</CardDescription>
          </CardHeader>
          <CardContent>
            {ticketBreakdown.length === 0 ? (
              <p className="text-sm text-muted-foreground">No data yet.</p>
            ) : (
              <BarChart items={ticketBreakdown} labelKey="label" valueKey="value" />
            )}
          </CardContent>
        </Card>
      </div>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Top customer questions</CardTitle>
          <CardDescription>Most frequent questions from chat widget</CardDescription>
        </CardHeader>
        <CardContent>
          {!analytics || analytics.topQuestions.length === 0 ? (
            <p className="text-sm text-muted-foreground">No questions recorded yet.</p>
          ) : (
            <ul className="space-y-3">
              {analytics.topQuestions.map((item, index) => (
                <li key={index} className="flex items-start justify-between gap-4 text-sm">
                  <span className="flex-1">{item.question}</span>
                  <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-xs font-medium">
                    {item.count}×
                  </span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </>
  );
}

function MetricCard({ title, value }: { title: string; value: string | number }) {
  return (
    <Card>
      <CardHeader className="pb-2">
        <CardDescription>{title}</CardDescription>
        <CardTitle className="text-3xl">{value}</CardTitle>
      </CardHeader>
    </Card>
  );
}
