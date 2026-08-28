"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ArrowLeftIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Spinner } from "@/components/ui/spinner";
import { ErrorState, LoadingNotice } from "@/components/ui/state";
import { useAuth } from "@/contexts/auth-context";
import { api, type MessageResponse } from "@/lib/api";
import { errorMessage, useAsync } from "@/lib/use-async";
import { cn, scrollToBottom } from "@/lib/utils";

const SUGGESTIONS = ["Where is my order #48291?", "I want a refund."];

export default function WidgetPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const session = useAsync(
    companyId
      ? () => api.startWidgetSession(companyId, "demo@customer.com", "Demo Customer")
      : null,
    [companyId]
  );

  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const sessionId = session.data?.id ?? null;
  const messages: MessageResponse[] = session.data?.messages ?? [];

  useEffect(() => {
    scrollToBottom(bottomRef.current);
  }, [messages.length]);

  async function handleSend(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || !sessionId || sending) return;

    const content = input.trim();
    setSending(true);
    setSendError(null);

    try {
      session.setData(await api.sendWidgetMessage(sessionId, content));
      setInput("");
    } catch (err) {
      setSendError(errorMessage(err, "Message not sent. Try again."));
    } finally {
      setSending(false);
    }
  }

  if (!companyId) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-4 p-6 text-center">
        <p className="text-sm text-muted-foreground">
          Log in to preview the chat widget for your company.
        </p>
        <Link href="/login">
          <Button>Log in</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center gap-4 p-4 sm:p-6">
      <div className="w-full max-w-md">
        <Link href="/dashboard">
          <Button variant="ghost" size="sm">
            <ArrowLeftIcon aria-hidden="true" />
            Back to dashboard
          </Button>
        </Link>
      </div>

      <Card className="flex h-[min(38rem,80dvh)] w-full max-w-md flex-col gap-0 overflow-hidden py-0">
        <div className="shrink-0 border-b px-4 py-3">
          <CardTitle>SupportIQ chat</CardTitle>
          <p className="text-xs text-muted-foreground">
            Ask about orders, refunds, or anything else.
          </p>
        </div>

        <div
          role="log"
          aria-live="polite"
          aria-label="Chat messages"
          className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4"
        >
          {session.status !== "ready" && session.status !== "error" && (
            <LoadingNotice label="Starting a chat session" slow={session.slow} />
          )}

          {session.status === "error" && (
            <ErrorState
              title="Chat unavailable"
              description={session.error}
              onRetry={session.reload}
            />
          )}

          {session.status === "ready" && messages.length === 0 && (
            <div className="space-y-3 py-4 text-center">
              <p className="text-sm text-muted-foreground">Try one of these to see it work:</p>
              <div className="flex flex-wrap justify-center gap-2">
                {SUGGESTIONS.map((suggestion) => (
                  <Button
                    key={suggestion}
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setInput(suggestion)}
                  >
                    {suggestion}
                  </Button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => (
            <div
              key={msg.id}
              className={cn("flex", msg.role === "CUSTOMER" ? "justify-end" : "justify-start")}
            >
              <div
                className={cn(
                  "max-w-[80%] rounded-lg px-3 py-2 text-sm whitespace-pre-wrap",
                  msg.role === "CUSTOMER" ? "bg-primary text-primary-foreground" : "bg-muted"
                )}
              >
                {msg.content}
              </div>
            </div>
          ))}

          {sending && (
            <div className="flex justify-start">
              <div className="flex items-center gap-2 rounded-lg bg-muted px-3 py-2 text-sm text-muted-foreground">
                <Spinner className="size-3.5" />
                Thinking…
              </div>
            </div>
          )}

          <div ref={bottomRef} />
        </div>

        <form onSubmit={handleSend} className="shrink-0 space-y-2 border-t p-4">
          <div className="flex gap-2">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type your message…"
              aria-label="Your message"
              aria-invalid={sendError ? true : undefined}
              disabled={session.status !== "ready" || sending}
            />
            <Button type="submit" disabled={session.status !== "ready" || sending || !input.trim()}>
              Send
            </Button>
          </div>
          {sendError && (
            <p role="alert" className="text-sm text-destructive">
              {sendError}
            </p>
          )}
        </form>
      </Card>
    </div>
  );
}
