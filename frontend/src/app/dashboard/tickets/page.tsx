"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/auth-context";
import { api, type CompanyMember, type TicketItem, ApiError } from "@/lib/api";

const STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"] as const;
const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"] as const;

function statusVariant(status: string) {
  if (status === "RESOLVED" || status === "CLOSED") return "secondary" as const;
  if (status === "IN_PROGRESS") return "default" as const;
  return "outline" as const;
}

export default function TicketsPage() {
  const { user } = useAuth();
  const [tickets, setTickets] = useState<TicketItem[]>([]);
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selected, setSelected] = useState<TicketItem | null>(null);
  const [note, setNote] = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [createPriority, setCreatePriority] = useState("MEDIUM");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!user?.companyId) return;
    api.getTickets(user.companyId).then(setTickets).catch(console.error);
    api.getCompanyMembers(user.companyId).then(setMembers).catch(console.error);
  }, [user?.companyId]);

  useEffect(() => {
    if (!user?.companyId || selectedId == null) {
      setSelected(null);
      return;
    }
    api.getTicket(selectedId, user.companyId).then(setSelected).catch(console.error);
  }, [user?.companyId, selectedId]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !subject.trim()) return;

    setLoading(true);
    setError("");
    try {
      const ticket = await api.createTicket(user.companyId, {
        subject: subject.trim(),
        description: description.trim() || undefined,
        priority: createPriority,
        customerEmail: customerEmail.trim() || undefined,
      });
      setTickets((prev) => [ticket, ...prev]);
      setSelectedId(ticket.id);
      setShowCreate(false);
      setSubject("");
      setDescription("");
      setCustomerEmail("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create ticket");
    } finally {
      setLoading(false);
    }
  }

  async function handleUpdate(field: "status" | "priority" | "assignedToUserId", value: string) {
    if (!user?.companyId || !selected) return;

    setLoading(true);
    try {
      const payload =
        field === "assignedToUserId"
          ? { assignedToUserId: value === "" ? 0 : Number(value) }
          : field === "status"
            ? { status: value }
            : { priority: value };

      const updated = await api.updateTicket(selected.id, user.companyId, payload);
      setSelected(updated);
      setTickets((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  async function handleAddNote(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !selected || !note.trim()) return;

    setLoading(true);
    try {
      const updated = await api.addTicketNote(selected.id, user.companyId, note.trim());
      setSelected(updated);
      setTickets((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
      setNote("");
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">Tickets</h1>
          <p className="mt-1 text-muted-foreground">Assign, update status, and add internal notes.</p>
        </div>
        <Button onClick={() => setShowCreate((v) => !v)}>
          {showCreate ? "Cancel" : "New ticket"}
        </Button>
      </div>

      {showCreate && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="text-base">Create ticket</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleCreate} className="space-y-4">
              {error && <p className="text-sm text-destructive">{error}</p>}
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="subject">Subject</Label>
                  <Input
                    id="subject"
                    value={subject}
                    onChange={(e) => setSubject(e.target.value)}
                    required
                  />
                </div>
                <div className="space-y-2 sm:col-span-2">
                  <Label htmlFor="description">Description</Label>
                  <Input
                    id="description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="customerEmail">Customer email</Label>
                  <Input
                    id="customerEmail"
                    type="email"
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="priority">Priority</Label>
                  <select
                    id="priority"
                    className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                    value={createPriority}
                    onChange={(e) => setCreatePriority(e.target.value)}
                  >
                    {PRIORITIES.map((p) => (
                      <option key={p} value={p}>
                        {p}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <Button type="submit" disabled={loading}>
                Create ticket
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <div className="mt-8 grid gap-4 lg:grid-cols-[320px_1fr]">
        <Card className="h-[640px] overflow-hidden">
          <CardHeader className="border-b py-4">
            <CardTitle className="text-base">Queue</CardTitle>
          </CardHeader>
          <CardContent className="h-[calc(100%-4rem)] overflow-y-auto p-0">
            {tickets.length === 0 ? (
              <p className="p-4 text-sm text-muted-foreground">No tickets yet.</p>
            ) : (
              <ul>
                {tickets.map((ticket) => (
                  <li key={ticket.id}>
                    <button
                      type="button"
                      onClick={() => setSelectedId(ticket.id)}
                      className={`w-full border-b px-4 py-3 text-left transition-colors hover:bg-muted/50 ${
                        selectedId === ticket.id ? "bg-muted" : ""
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate text-sm font-medium">{ticket.subject}</span>
                        <Badge variant={statusVariant(ticket.status)}>{ticket.status}</Badge>
                      </div>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {ticket.priority} · {ticket.customerEmail || "No email"}
                      </p>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card className="flex h-[640px] flex-col overflow-hidden">
          {!selected ? (
            <CardContent className="flex flex-1 items-center justify-center text-sm text-muted-foreground">
              Select a ticket to view details.
            </CardContent>
          ) : (
            <>
              <CardHeader className="border-b py-4">
                <CardTitle className="text-base">{selected.subject}</CardTitle>
                <p className="text-xs text-muted-foreground">
                  Created {new Date(selected.createdAt).toLocaleString()}
                </p>
              </CardHeader>
              <CardContent className="flex flex-1 flex-col overflow-hidden p-4">
                <div className="space-y-4 overflow-y-auto">
                  <div>
                    <p className="text-xs font-medium uppercase text-muted-foreground">Description</p>
                    <p className="mt-1 text-sm whitespace-pre-wrap">
                      {selected.description || "No description provided."}
                    </p>
                  </div>

                  {selected.conversationId && (
                    <Link
                      href="/dashboard/conversations"
                      className="text-sm text-primary hover:underline"
                    >
                      Linked to conversation #{selected.conversationId}
                    </Link>
                  )}

                  <div className="grid gap-4 sm:grid-cols-3">
                    <div className="space-y-2">
                      <Label>Status</Label>
                      <select
                        className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                        value={selected.status}
                        onChange={(e) => handleUpdate("status", e.target.value)}
                        disabled={loading}
                      >
                        {STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <Label>Priority</Label>
                      <select
                        className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                        value={selected.priority}
                        onChange={(e) => handleUpdate("priority", e.target.value)}
                        disabled={loading}
                      >
                        {PRIORITIES.map((p) => (
                          <option key={p} value={p}>
                            {p}
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <Label>Assignee</Label>
                      <select
                        className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm"
                        value={selected.assignedToUserId ?? ""}
                        onChange={(e) => handleUpdate("assignedToUserId", e.target.value)}
                        disabled={loading}
                      >
                        <option value="">Unassigned</option>
                        {members.map((m) => (
                          <option key={m.userId} value={m.userId}>
                            {m.firstName} {m.lastName}
                          </option>
                        ))}
                      </select>
                    </div>
                  </div>

                  <div>
                    <p className="text-xs font-medium uppercase text-muted-foreground">Internal notes</p>
                    <pre className="mt-2 max-h-48 overflow-y-auto rounded-md bg-muted p-3 text-xs whitespace-pre-wrap">
                      {selected.internalNotes || "No internal notes yet."}
                    </pre>
                  </div>
                </div>

                <form onSubmit={handleAddNote} className="mt-4 flex gap-2 border-t pt-4">
                  <Input
                    value={note}
                    onChange={(e) => setNote(e.target.value)}
                    placeholder="Add internal note..."
                    disabled={loading}
                  />
                  <Button type="submit" disabled={loading || !note.trim()}>
                    Add note
                  </Button>
                </form>
              </CardContent>
            </>
          )}
        </Card>
      </div>
    </>
  );
}
