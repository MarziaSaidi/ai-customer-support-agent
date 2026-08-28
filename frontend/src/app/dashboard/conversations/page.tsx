"use client";

import { useEffect, useRef, useState } from "react";
import { ArrowLeftIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { EmptyState, ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { useAuth } from "@/contexts/auth-context";
import { api, type MessageResponse } from "@/lib/api";
import { errorMessage, useAsync } from "@/lib/use-async";
import { cn, scrollToBottom } from "@/lib/utils";

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

/** Each sender gets one distinct treatment — position, surface, or emphasis. */
function messageStyle(role: string) {
  switch (role) {
    case "CUSTOMER":
      return "bg-primary text-primary-foreground";
    case "AGENT":
      return "bg-info/10 text-foreground ring-1 ring-info/25";
    case "SYSTEM":
      return "bg-muted text-muted-foreground italic";
    default:
      return "bg-muted text-foreground";
  }
}

export default function ConversationsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [reply, setReply] = useState("");
  const [sending, setSending] = useState(false);
  const [replyError, setReplyError] = useState<string | null>(null);
  const [resolving, setResolving] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  const inbox = useAsync(companyId ? () => api.getSessions(companyId) : null, [companyId]);

  const detail = useAsync(
    companyId && selectedId != null ? () => api.getSession(selectedId, companyId) : null,
    [companyId, selectedId]
  );

  const activeConversation = detail.data;

  useEffect(() => {
    scrollToBottom(bottomRef.current);
  }, [activeConversation?.messages]);

  async function refreshList() {
    if (!companyId) return;
    try {
      inbox.setData(await api.getSessions(companyId));
    } catch {
      /* The list is still usable; the detail pane already reflects the change. */
    }
  }

  async function handleReply(e: React.FormEvent) {
    e.preventDefault();
    if (selectedId == null || !reply.trim() || sending) return;

    const content = reply.trim();
    setSending(true);
    setReplyError(null);

    try {
      detail.setData(await api.sendAgentMessage(selectedId, content));
      setReply("");
      await refreshList();
    } catch (err) {
      // Keep what they typed — losing a drafted reply is worse than the error.
      setReplyError(errorMessage(err, "Message not sent. Try again."));
    } finally {
      setSending(false);
    }
  }

  async function handleResolve() {
    if (selectedId == null || resolving) return;

    setResolving(true);
    try {
      detail.setData(await api.resolveSession(selectedId));
      await refreshList();
    } catch (err) {
      setReplyError(errorMessage(err, "Couldn't mark this resolved."));
    } finally {
      setResolving(false);
    }
  }

  const canReply =
    activeConversation && !activeConversation.resolved && activeConversation.status !== "RESOLVED";

  const panelHeight = "h-[70dvh] lg:h-[clamp(30rem,68vh,40rem)]";

  return (
    <>
      <PageHeader
        title="Conversations"
        description="Review customer chats and reply as an agent."
      />

      <div className="grid gap-4 lg:grid-cols-[minmax(0,320px)_1fr]">
        {/* On narrow screens the list and the thread take turns rather than
            stacking two fixed-height panels. */}
        <Card
          className={cn(
            "flex flex-col gap-0 overflow-hidden py-0",
            panelHeight,
            selectedId != null && "hidden lg:flex"
          )}
        >
          <div className="shrink-0 border-b px-4 py-3">
            <CardTitle>Inbox</CardTitle>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto">
            {inbox.status === "loading" && (
              <div className="p-4">
                <SkeletonRows rows={5} />
                <LoadingNotice className="mt-4" label="Loading conversations" slow={inbox.slow} />
              </div>
            )}

            {inbox.status === "error" && (
              <ErrorState
                className="m-4"
                description={inbox.error}
                onRetry={inbox.reload}
              />
            )}

            {inbox.status === "ready" &&
              (inbox.data && inbox.data.length > 0 ? (
                <ul>
                  {inbox.data.map((item) => {
                    const selected = selectedId === item.id;
                    return (
                      <li key={item.id}>
                        <button
                          type="button"
                          aria-current={selected ? "true" : undefined}
                          onClick={() => setSelectedId(item.id)}
                          className={cn(
                            "relative w-full border-b px-4 py-3 text-left transition-colors outline-none",
                            "before:absolute before:inset-y-0 before:left-0 before:w-0.5 before:transition-colors",
                            "hover:bg-muted/50 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:ring-inset",
                            selected ? "bg-muted before:bg-foreground" : "before:bg-transparent"
                          )}
                        >
                          <span className="flex items-center justify-between gap-2">
                            <span className="truncate text-sm font-medium">
                              {item.customerName ||
                                item.customerEmail ||
                                `Conversation #${item.id}`}
                            </span>
                            <Badge variant={item.resolved ? "success" : "secondary"}>
                              {item.status}
                            </Badge>
                          </span>
                          <span className="mt-1 block truncate text-xs text-muted-foreground">
                            {item.lastMessagePreview || "No messages yet"}
                          </span>
                          <span className="mt-0.5 block text-xs text-muted-foreground tabular-nums">
                            {item.messageCount} messages
                          </span>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <EmptyState
                  className="p-4"
                  title="No conversations yet"
                  description="Chats started from your widget land here."
                />
              ))}
          </div>
        </Card>

        <Card
          className={cn(
            "flex flex-col gap-0 overflow-hidden py-0",
            panelHeight,
            selectedId == null && "hidden lg:flex"
          )}
        >
          {detail.status === "loading" && (
            <div className="flex flex-1 items-center justify-center p-4">
              <LoadingNotice label="Opening conversation" slow={detail.slow} />
            </div>
          )}

          {detail.status === "error" && (
            <div className="flex flex-1 items-center justify-center p-4">
              <ErrorState description={detail.error} onRetry={detail.reload} />
            </div>
          )}

          {detail.status === "idle" && (
            <div className="flex flex-1 items-center justify-center p-6 text-center text-sm text-muted-foreground">
              Select a conversation to view its messages.
            </div>
          )}

          {detail.status === "ready" && activeConversation && (
            <>
              <div className="shrink-0 border-b px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex min-w-0 items-start gap-2">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label="Back to inbox"
                      className="lg:hidden"
                      onClick={() => setSelectedId(null)}
                    >
                      <ArrowLeftIcon aria-hidden="true" />
                    </Button>
                    <div className="min-w-0">
                      <CardTitle className="truncate">
                        {activeConversation.customerName ||
                          activeConversation.customerEmail ||
                          `Conversation #${activeConversation.id}`}
                      </CardTitle>
                      <p className="text-xs text-muted-foreground">
                        Started {new Date(activeConversation.createdAt).toLocaleString()}
                      </p>
                    </div>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <Badge variant={activeConversation.resolved ? "success" : "secondary"}>
                      {activeConversation.status}
                    </Badge>
                    {canReply && (
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={handleResolve}
                        disabled={resolving}
                      >
                        {resolving && <Spinner className="size-3.5" />}
                        {resolving ? "Resolving" : "Mark resolved"}
                      </Button>
                    )}
                  </div>
                </div>
              </div>

              <div
                role="log"
                aria-live="polite"
                aria-label="Conversation messages"
                className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4"
              >
                {activeConversation.messages.map((msg: MessageResponse) => (
                  <div
                    key={msg.id}
                    className={cn(
                      "flex",
                      msg.role === "CUSTOMER" ? "justify-end" : "justify-start"
                    )}
                  >
                    <div
                      className={cn(
                        "max-w-[85%] rounded-lg px-3 py-2 text-sm",
                        messageStyle(msg.role)
                      )}
                    >
                      <p
                        className={cn(
                          "text-eyebrow mb-1",
                          msg.role === "CUSTOMER"
                            ? "text-primary-foreground/80"
                            : "text-muted-foreground"
                        )}
                      >
                        {roleLabel(msg.role)}
                      </p>
                      <p className="whitespace-pre-wrap">{msg.content}</p>
                    </div>
                  </div>
                ))}
                <div ref={bottomRef} />
              </div>

              {canReply ? (
                <form onSubmit={handleReply} className="shrink-0 space-y-2 border-t p-4">
                  <div className="flex gap-2">
                    <Input
                      value={reply}
                      onChange={(e) => setReply(e.target.value)}
                      placeholder="Reply as agent…"
                      aria-label="Reply as agent"
                      aria-invalid={replyError ? true : undefined}
                      disabled={sending}
                    />
                    <Button type="submit" disabled={sending || !reply.trim()}>
                      {sending && <Spinner className="size-3.5" />}
                      {sending ? "Sending" : "Send"}
                    </Button>
                  </div>
                  {replyError && (
                    <p role="alert" className="text-sm text-destructive">
                      {replyError}
                    </p>
                  )}
                </form>
              ) : (
                <p className="shrink-0 border-t p-4 text-sm text-muted-foreground">
                  This conversation is resolved.
                </p>
              )}
            </>
          )}
        </Card>
      </div>
    </>
  );
}
