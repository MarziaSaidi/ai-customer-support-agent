"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/auth-context";
import { api, type ConversationSummary, type ChatSessionResponse, type MessageResponse } from "@/lib/api";

function roleLabel(role: string) {
  switch (role) {
    case "CUSTOMER":
      return "Customer";
    case "AGENT":
      return "Agent";
    case "AI":
      return "AI";
    case "SYSTEM":
      return "System";
    default:
      return role;
  }
}

function messageAlignment(role: string) {
  if (role === "CUSTOMER") return "justify-end";
  if (role === "AGENT") return "justify-start";
  return "justify-start";
}

function messageStyle(role: string) {
  if (role === "CUSTOMER") return "bg-primary text-primary-foreground";
  if (role === "AGENT") return "bg-blue-100 text-blue-950 dark:bg-blue-950 dark:text-blue-100";
  if (role === "SYSTEM") return "bg-muted text-muted-foreground italic";
  return "bg-muted";
}

export default function ConversationsPage() {
  const { user } = useAuth();
  const [summaries, setSummaries] = useState<ConversationSummary[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [conversation, setConversation] = useState<ChatSessionResponse | null>(null);
  const [reply, setReply] = useState("");
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!user?.companyId) return;
    api.getSessions(user.companyId).then(setSummaries).catch(console.error);
  }, [user?.companyId]);

  useEffect(() => {
    if (!user?.companyId || selectedId == null) return;

    let cancelled = false;
    api
      .getSession(selectedId, user.companyId)
      .then((data) => {
        if (!cancelled) setConversation(data);
      })
      .catch(console.error);

    return () => {
      cancelled = true;
    };
  }, [user?.companyId, selectedId]);

  const activeConversation =
    selectedId != null && conversation?.id === selectedId ? conversation : null;

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [activeConversation?.messages]);

  async function refreshList() {
    if (!user?.companyId) return;
    const list = await api.getSessions(user.companyId);
    setSummaries(list);
  }

  async function handleReply(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || selectedId == null || !reply.trim() || loading) return;

    setLoading(true);
    const content = reply.trim();
    setReply("");

    try {
      const updated = await api.sendAgentMessage(selectedId, content);
      setConversation(updated);
      await refreshList();
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleResolve() {
    if (!user?.companyId || selectedId == null || actionLoading) return;

    setActionLoading(true);
    try {
      const updated = await api.resolveSession(selectedId);
      setConversation(updated);
      await refreshList();
    } catch (err) {
      console.error(err);
    } finally {
      setActionLoading(false);
    }
  }

  const canReply =
    activeConversation &&
    !activeConversation.resolved &&
    activeConversation.status !== "RESOLVED";

  return (
    <>
      <h1 className="text-2xl font-bold">Conversations</h1>
      <p className="mt-1 text-muted-foreground">Review customer chats and reply as an agent.</p>

      <div className="mt-8 grid gap-4 lg:grid-cols-[320px_1fr]">
        <Card className="h-[640px] overflow-hidden">
          <CardHeader className="border-b py-4">
            <CardTitle className="text-base">Inbox</CardTitle>
          </CardHeader>
          <CardContent className="h-[calc(100%-4rem)] overflow-y-auto p-0">
            {summaries.length === 0 ? (
              <p className="p-4 text-sm text-muted-foreground">No conversations yet.</p>
            ) : (
              <ul>
                {summaries.map((item) => (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedId(item.id)}
                      className={`w-full border-b px-4 py-3 text-left transition-colors hover:bg-muted/50 ${
                        selectedId === item.id ? "bg-muted" : ""
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm font-medium">
                          {item.customerName || item.customerEmail || `Conversation #${item.id}`}
                        </span>
                        <Badge variant={item.resolved ? "secondary" : "default"}>{item.status}</Badge>
                      </div>
                      <p className="mt-1 truncate text-xs text-muted-foreground">
                        {item.lastMessagePreview || "No messages yet"}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">{item.messageCount} messages</p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card className="flex h-[640px] flex-col overflow-hidden">
          {!activeConversation ? (
            <CardContent className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
              Select a conversation to view messages.
            </CardContent>
          ) : (
            <>
              <CardHeader className="border-b py-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <CardTitle className="text-base">
                      {activeConversation.customerName || activeConversation.customerEmail || `Conversation #${activeConversation.id}`}
                    </CardTitle>
                    <p className="text-xs text-muted-foreground">
                      Started {new Date(activeConversation.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={activeConversation.resolved ? "secondary" : "default"}>{activeConversation.status}</Badge>
                    {canReply && (
                      <Button size="sm" variant="outline" onClick={handleResolve} disabled={actionLoading}>
                        Mark resolved
                      </Button>
                    )}
                  </div>
                </div>
              </CardHeader>
              <CardContent className="flex flex-1 flex-col overflow-hidden p-0">
                <div className="flex-1 space-y-3 overflow-y-auto p-4">
                  {activeConversation.messages.map((msg: MessageResponse) => (
                    <div key={msg.id} className={`flex ${messageAlignment(msg.role)}`}>
                      <div className={`max-w-[85%] rounded-lg px-3 py-2 text-sm ${messageStyle(msg.role)}`}>
                        <p className="mb-1 text-[10px] font-medium uppercase opacity-70">{roleLabel(msg.role)}</p>
                        <p className="whitespace-pre-wrap">{msg.content}</p>
                      </div>
                    </div>
                  ))}
                  <div ref={bottomRef} />
                </div>
                {canReply ? (
                  <form onSubmit={handleReply} className="flex gap-2 border-t p-4">
                    <Input
                      value={reply}
                      onChange={(e) => setReply(e.target.value)}
                      placeholder="Reply as agent..."
                      disabled={loading}
                    />
                    <Button type="submit" disabled={loading || !reply.trim()}>
                      Send
                    </Button>
                  </form>
                ) : (
                  <div className="border-t p-4 text-sm text-muted-foreground">
                    This conversation is resolved.
                  </div>
                )}
              </CardContent>
            </>
          )}
        </Card>
      </div>

      <div className="mt-4">
        <Link href="/widget">
          <Button variant="outline" size="sm">
            Open chat widget preview
          </Button>
        </Link>
      </div>
    </>
  );
}
