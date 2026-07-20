"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useAuth } from "@/contexts/auth-context";
import { api, type AnalyticsResponse, type ConversationSummary } from "@/lib/api";

export default function DashboardPage() {
  const { user } = useAuth();
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null);
  const [sessions, setSessions] = useState<ConversationSummary[]>([]);

  useEffect(() => {
    if (!user?.companyId) return;

    api.getAnalytics(user.companyId).then(setAnalytics).catch(console.error);
    api.getSessions(user.companyId).then(setSessions).catch(console.error);
  }, [user?.companyId]);

  return (
    <>
      <h1 className="text-2xl font-bold">Overview</h1>
      <p className="mt-1 text-muted-foreground">Monitor conversations, tickets, and AI performance.</p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard title="Conversations" value={analytics?.totalConversations ?? "—"} />
        <MetricCard title="AI Resolution Rate" value={analytics ? `${analytics.aiResolutionRate.toFixed(1)}%` : "—"} />
        <MetricCard title="Open Tickets" value={analytics?.openTickets ?? "—"} />
        <MetricCard title="Satisfaction" value={analytics ? analytics.customerSatisfaction.toFixed(1) : "—"} />
      </div>

      <Separator className="my-8" />

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Recent Conversations</CardTitle>
            <CardDescription>Latest customer chat sessions</CardDescription>
          </CardHeader>
          <CardContent>
            {sessions.length === 0 ? (
              <p className="text-sm text-muted-foreground">No conversations yet.</p>
            ) : (
              <ul className="space-y-3">
                {sessions.slice(0, 5).map((session) => (
                  <li key={session.id} className="flex items-center justify-between text-sm">
                    <span>{session.customerName || session.customerEmail || `Conversation #${session.id}`}</span>
                    <Badge variant={session.resolved ? "secondary" : "default"}>{session.status}</Badge>
                  </li>
                ))}
              </ul>
            )}
            {sessions.length > 0 && (
              <Link href="/dashboard/conversations" className="mt-4 inline-block text-sm text-primary hover:underline">
                View all conversations
              </Link>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Quick Actions</CardTitle>
            <CardDescription>Manage your support platform</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <Link href="/dashboard/tickets">
              <Button variant="outline" className="w-full justify-start">
                View support tickets
              </Button>
            </Link>
            <Link href="/widget">
              <Button variant="outline" className="w-full justify-start">
                Preview chat widget
              </Button>
            </Link>
            <Link href="/dashboard/team">
              <Button variant="outline" className="w-full justify-start">
                Manage team members
              </Button>
            </Link>
            <Link href="/dashboard/settings">
              <Button variant="outline" className="w-full justify-start">
                Configure AI settings
              </Button>
            </Link>
            <Link href="/dashboard/documents">
              <Button variant="outline" className="w-full justify-start">
                Upload documentation
              </Button>
            </Link>
          </CardContent>
        </Card>
      </div>
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
