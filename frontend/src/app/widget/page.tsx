"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useAuth } from "@/contexts/auth-context";
import { api, type MessageResponse } from "@/lib/api";

export default function WidgetPage() {
  const { user } = useAuth();
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!user?.companyId) return;

    api
      .startWidgetSession(user.companyId, "demo@customer.com", "Demo Customer")
      .then((session) => {
        setSessionId(session.id);
        setMessages(session.messages);
      })
      .catch(console.error);
  }, [user?.companyId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function handleSend(e: React.FormEvent) {
    e.preventDefault();
    if (!input.trim() || !sessionId || loading) return;

    setLoading(true);
    const content = input.trim();
    setInput("");

    try {
      const session = await api.sendWidgetMessage(sessionId, content);
      setMessages(session.messages);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  if (!user?.companyId) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center p-6">
        <p className="text-sm text-muted-foreground">Log in to preview the chat widget for your company.</p>
        <Link href="/login" className="mt-4">
          <Button>Log in</Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center p-6">
      <div className="mb-4">
        <Link href="/dashboard">
          <Button variant="ghost" size="sm">
            &larr; Back to dashboard
          </Button>
        </Link>
      </div>

      <Card className="flex h-[600px] w-full max-w-md flex-col">
        <CardHeader className="border-b py-4">
          <CardTitle className="text-base">SupportIQ Chat</CardTitle>
          <p className="text-xs text-muted-foreground">Ask about orders, refunds, or anything else.</p>
        </CardHeader>
        <CardContent className="flex flex-1 flex-col overflow-hidden p-0">
          <div className="flex-1 space-y-3 overflow-y-auto p-4">
            {messages.length === 0 && (
              <p className="text-center text-sm text-muted-foreground">
                Try: &quot;Where is my order #48291?&quot; or &quot;I want a refund.&quot;
              </p>
            )}
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex ${msg.role === "CUSTOMER" ? "justify-end" : "justify-start"}`}
              >
                <div
                  className={`max-w-[80%] rounded-lg px-3 py-2 text-sm ${
                    msg.role === "CUSTOMER"
                      ? "bg-primary text-primary-foreground"
                      : "bg-muted"
                  }`}
                >
                  {msg.content}
                </div>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>
          <form onSubmit={handleSend} className="flex gap-2 border-t p-4">
            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Type your message..."
              disabled={!sessionId || loading}
            />
            <Button type="submit" disabled={!sessionId || loading || !input.trim()}>
              Send
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
