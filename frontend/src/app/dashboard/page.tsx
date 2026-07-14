"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { api, type AnalyticsResponse, type AuthResponse, type ChatSessionResponse } from "@/lib/api";

function getStoredUser(): AuthResponse | null {
  if (typeof window === "undefined") return null;
  const stored = localStorage.getItem("user");
  return stored ? JSON.parse(stored) : null;
}

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<AuthResponse | null>(getStoredUser);
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null);
  const [sessions, setSessions] = useState<ChatSessionResponse[]>([]);

  useEffect(() => {
    if (!user) {
      router.push("/login");
      return;
    }

    if (user.companyId) {
      api.getAnalytics(user.companyId).then(setAnalytics).catch(console.error);
      api.getSessions(user.companyId).then(setSessions).catch(console.error);
    }
  }, [user, router]);

  function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
    setUser(null);
    router.push("/login");
  }

  if (!user) return null;

  return (
    <div className="flex flex-1 flex-col">
      <header className="border-b">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-sm font-bold">
              SA
            </div>
            <span className="text-lg font-semibold">SupportAI</span>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm text-muted-foreground">
              {user.firstName} {user.lastName}
            </span>
            <Badge variant="secondary">{user.role}</Badge>
            <Button variant="outline" size="sm" onClick={handleLogout}>
              Log out
            </Button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-6 py-8">
        <h1 className="text-2xl font-bold">Dashboard</h1>
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
                      <span>{session.customerName || session.customerEmail || `Session #${session.id}`}</span>
                      <Badge variant={session.resolved ? "secondary" : "default"}>{session.status}</Badge>
                    </li>
                  ))}
                </ul>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Quick Actions</CardTitle>
              <CardDescription>Manage your support platform</CardDescription>
            </CardHeader>
            <CardContent className="space-y-3">
              <Link href="/widget">
                <Button variant="outline" className="w-full justify-start">
                  Preview chat widget
                </Button>
              </Link>
              <Button variant="outline" className="w-full justify-start" disabled>
                Upload documentation
              </Button>
              <Button variant="outline" className="w-full justify-start" disabled>
                Manage team members
              </Button>
              <Button variant="outline" className="w-full justify-start" disabled>
                Configure AI settings
              </Button>
            </CardContent>
          </Card>
        </div>
      </main>
    </div>
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
