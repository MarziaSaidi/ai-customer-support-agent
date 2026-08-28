"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { ArrowLeftIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { EmptyState, ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { useAuth } from "@/contexts/auth-context";
import { api, type TicketItem } from "@/lib/api";
import { errorMessage, useAsync } from "@/lib/use-async";
import { cn } from "@/lib/utils";

const STATUSES = ["OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED"] as const;
const PRIORITIES = ["LOW", "MEDIUM", "HIGH", "URGENT"] as const;

type EditableField = "status" | "priority" | "assignedToUserId";

function statusVariant(status: string) {
  if (status === "RESOLVED" || status === "CLOSED") return "success" as const;
  if (status === "IN_PROGRESS") return "default" as const;
  return "secondary" as const;
}

function priorityVariant(priority: string) {
  if (priority === "URGENT") return "destructive" as const;
  if (priority === "HIGH") return "warning" as const;
  return "outline" as const;
}

function humanize(value: string) {
  return value.charAt(0) + value.slice(1).toLowerCase().replace(/_/g, " ");
}

export default function TicketsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;

  const [selectedId, setSelectedId] = useState<number | null>(null);

  const [note, setNote] = useState("");
  const [noteSaving, setNoteSaving] = useState(false);

  const [showCreate, setShowCreate] = useState(false);
  const [subject, setSubject] = useState("");
  const [description, setDescription] = useState("");
  const [customerEmail, setCustomerEmail] = useState("");
  const [createPriority, setCreatePriority] = useState("MEDIUM");
  const [createError, setCreateError] = useState("");
  const [creating, setCreating] = useState(false);

  const [pendingField, setPendingField] = useState<EditableField | null>(null);
  const [savedField, setSavedField] = useState<EditableField | null>(null);
  const [updateError, setUpdateError] = useState<string | null>(null);
  const savedTimer = useRef<number | undefined>(undefined);

  const tickets = useAsync(companyId ? () => api.getTickets(companyId) : null, [companyId]);
  const members = useAsync(
    companyId ? () => api.getCompanyMembers(companyId) : null,
    [companyId]
  );

  useEffect(() => () => window.clearTimeout(savedTimer.current), []);

  const detail = useAsync(
    companyId && selectedId != null ? () => api.getTicket(selectedId, companyId) : null,
    [companyId, selectedId]
  );

  const activeTicket = detail.data;

  function applyUpdate(updated: TicketItem) {
    detail.setData(updated);
    tickets.setData(
      (tickets.data ?? []).map((t) => (t.id === updated.id ? updated : t))
    );
  }

  function flagSaved(field: EditableField) {
    setSavedField(field);
    window.clearTimeout(savedTimer.current);
    savedTimer.current = window.setTimeout(() => setSavedField(null), 2500);
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (!companyId || !subject.trim()) return;

    setCreating(true);
    setCreateError("");
    try {
      const ticket = await api.createTicket(companyId, {
        subject: subject.trim(),
        description: description.trim() || undefined,
        priority: createPriority,
        customerEmail: customerEmail.trim() || undefined,
      });
      tickets.setData([ticket, ...(tickets.data ?? [])]);
      setSelectedId(ticket.id);
      setShowCreate(false);
      setSubject("");
      setDescription("");
      setCustomerEmail("");
    } catch (err) {
      setCreateError(errorMessage(err, "Failed to create ticket"));
    } finally {
      setCreating(false);
    }
  }

  async function handleUpdate(field: EditableField, value: string) {
    if (!companyId || !activeTicket) return;

    setPendingField(field);
    setUpdateError(null);
    try {
      const payload =
        field === "assignedToUserId"
          ? { assignedToUserId: value === "" ? 0 : Number(value) }
          : field === "status"
            ? { status: value }
            : { priority: value };

      applyUpdate(await api.updateTicket(activeTicket.id, companyId, payload));
      flagSaved(field);
    } catch (err) {
      setUpdateError(errorMessage(err, "Change not saved."));
    } finally {
      setPendingField(null);
    }
  }

  async function handleAddNote(e: React.FormEvent) {
    e.preventDefault();
    if (!companyId || !activeTicket || !note.trim()) return;

    setNoteSaving(true);
    setUpdateError(null);
    try {
      applyUpdate(await api.addTicketNote(activeTicket.id, companyId, note.trim()));
      setNote("");
    } catch (err) {
      setUpdateError(errorMessage(err, "Note not saved. Try again."));
    } finally {
      setNoteSaving(false);
    }
  }

  const panelHeight = "h-[70dvh] lg:h-[clamp(30rem,68vh,40rem)]";

  return (
    <>
      <PageHeader
        title="Tickets"
        description="Assign, update status, and add internal notes."
        action={
          <Button
            onClick={() => setShowCreate((v) => !v)}
            aria-expanded={showCreate}
            aria-controls="create-ticket-panel"
          >
            {showCreate ? "Cancel" : "New ticket"}
          </Button>
        }
      />

      {showCreate && (
        <Card id="create-ticket-panel" className="animate-enter">
          <div className="px-4">
            <CardTitle>Create ticket</CardTitle>
          </div>
          <div className="px-4">
            <form onSubmit={handleCreate} className="space-y-4">
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
                  <Label htmlFor="description">
                    Description <span className="text-muted-foreground">(optional)</span>
                  </Label>
                  <Textarea
                    id="description"
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="customerEmail">
                    Customer email <span className="text-muted-foreground">(optional)</span>
                  </Label>
                  <Input
                    id="customerEmail"
                    type="email"
                    autoComplete="email"
                    value={customerEmail}
                    onChange={(e) => setCustomerEmail(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="priority">Priority</Label>
                  <Select
                    id="priority"
                    value={createPriority}
                    onChange={(e) => setCreatePriority(e.target.value)}
                  >
                    {PRIORITIES.map((p) => (
                      <option key={p} value={p}>
                        {humanize(p)}
                      </option>
                    ))}
                  </Select>
                </div>
              </div>
              {createError && (
                <p role="alert" className="text-sm text-destructive">
                  {createError}
                </p>
              )}
              <Button type="submit" disabled={creating || !subject.trim()}>
                {creating && <Spinner className="size-3.5" />}
                {creating ? "Creating" : "Create ticket"}
              </Button>
            </form>
          </div>
        </Card>
      )}

      <div className="grid gap-4 lg:grid-cols-[minmax(0,320px)_1fr]">
        <Card
          className={cn(
            "flex flex-col gap-0 overflow-hidden py-0",
            panelHeight,
            selectedId != null && "hidden lg:flex"
          )}
        >
          <div className="shrink-0 border-b px-4 py-3">
            <CardTitle>Queue</CardTitle>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto">
            {tickets.status === "loading" && (
              <div className="p-4">
                <SkeletonRows rows={5} />
                <LoadingNotice className="mt-4" label="Loading tickets" slow={tickets.slow} />
              </div>
            )}

            {tickets.status === "error" && (
              <ErrorState className="m-4" description={tickets.error} onRetry={tickets.reload} />
            )}

            {tickets.status === "ready" &&
              (tickets.data && tickets.data.length > 0 ? (
                <ul>
                  {tickets.data.map((ticket) => {
                    const isSelected = selectedId === ticket.id;
                    return (
                      <li key={ticket.id}>
                        <button
                          type="button"
                          aria-current={isSelected ? "true" : undefined}
                          onClick={() => setSelectedId(ticket.id)}
                          className={cn(
                            "relative w-full border-b px-4 py-3 text-left transition-colors outline-none",
                            "before:absolute before:inset-y-0 before:left-0 before:w-0.5 before:transition-colors",
                            "hover:bg-muted/50 focus-visible:ring-3 focus-visible:ring-ring/50 focus-visible:ring-inset",
                            isSelected ? "bg-muted before:bg-foreground" : "before:bg-transparent"
                          )}
                        >
                          <span className="flex items-center justify-between gap-2">
                            <span className="truncate text-sm font-medium">{ticket.subject}</span>
                            <Badge variant={statusVariant(ticket.status)}>
                              {humanize(ticket.status)}
                            </Badge>
                          </span>
                          <span className="mt-1.5 flex items-center gap-2">
                            <Badge variant={priorityVariant(ticket.priority)}>
                              {humanize(ticket.priority)}
                            </Badge>
                            <span className="truncate text-xs text-muted-foreground">
                              {ticket.customerEmail || "No email"}
                            </span>
                          </span>
                        </button>
                      </li>
                    );
                  })}
                </ul>
              ) : (
                <EmptyState
                  className="p-4"
                  title="No tickets yet"
                  description="Create one, or let the AI escalate a conversation."
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
              <LoadingNotice label="Opening ticket" slow={detail.slow} />
            </div>
          )}

          {detail.status === "error" && (
            <div className="flex flex-1 items-center justify-center p-4">
              <ErrorState description={detail.error} onRetry={detail.reload} />
            </div>
          )}

          {detail.status === "idle" && (
            <div className="flex flex-1 items-center justify-center p-6 text-center text-sm text-muted-foreground">
              Select a ticket to view its details.
            </div>
          )}

          {detail.status === "ready" && activeTicket && (
            <>
              <div className="flex shrink-0 items-start gap-2 border-b px-4 py-3">
                <Button
                  variant="ghost"
                  size="icon-sm"
                  aria-label="Back to queue"
                  className="lg:hidden"
                  onClick={() => setSelectedId(null)}
                >
                  <ArrowLeftIcon aria-hidden="true" />
                </Button>
                <div className="min-w-0">
                  <CardTitle className="truncate">{activeTicket.subject}</CardTitle>
                  <p className="text-xs text-muted-foreground">
                    Created {new Date(activeTicket.createdAt).toLocaleString()}
                  </p>
                </div>
              </div>

              <div className="min-h-0 flex-1 space-y-5 overflow-y-auto p-4">
                <div>
                  <h3 className="text-eyebrow text-muted-foreground">Description</h3>
                  <p className="mt-1.5 text-sm whitespace-pre-wrap">
                    {activeTicket.description || "No description provided."}
                  </p>
                </div>

                {activeTicket.conversationId && (
                  <Link
                    href="/dashboard/conversations"
                    className="inline-block rounded-md text-sm font-medium underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
                  >
                    Linked to conversation #{activeTicket.conversationId}
                  </Link>
                )}

                <div className="grid gap-4 sm:grid-cols-3">
                  <div className="space-y-2">
                    <Label htmlFor="ticket-status">
                      Status
                      <FieldState field="status" pending={pendingField} saved={savedField} />
                    </Label>
                    <Select
                      id="ticket-status"
                      value={activeTicket.status}
                      onChange={(e) => handleUpdate("status", e.target.value)}
                      disabled={pendingField !== null}
                    >
                      {STATUSES.map((s) => (
                        <option key={s} value={s}>
                          {humanize(s)}
                        </option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ticket-priority">
                      Priority
                      <FieldState field="priority" pending={pendingField} saved={savedField} />
                    </Label>
                    <Select
                      id="ticket-priority"
                      value={activeTicket.priority}
                      onChange={(e) => handleUpdate("priority", e.target.value)}
                      disabled={pendingField !== null}
                    >
                      {PRIORITIES.map((p) => (
                        <option key={p} value={p}>
                          {humanize(p)}
                        </option>
                      ))}
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="ticket-assignee">
                      Assignee
                      <FieldState
                        field="assignedToUserId"
                        pending={pendingField}
                        saved={savedField}
                      />
                    </Label>
                    <Select
                      id="ticket-assignee"
                      value={activeTicket.assignedToUserId ?? ""}
                      onChange={(e) => handleUpdate("assignedToUserId", e.target.value)}
                      disabled={pendingField !== null}
                    >
                      <option value="">Unassigned</option>
                      {(members.data ?? []).map((m) => (
                        <option key={m.userId} value={m.userId}>
                          {m.firstName} {m.lastName}
                        </option>
                      ))}
                    </Select>
                  </div>
                </div>

                {updateError && (
                  <p role="alert" className="text-sm text-destructive">
                    {updateError}
                  </p>
                )}

                <div>
                  <h3 className="text-eyebrow text-muted-foreground">Internal notes</h3>
                  <p className="mt-1.5 rounded-md bg-muted p-3 text-xs whitespace-pre-wrap">
                    {activeTicket.internalNotes || "No internal notes yet."}
                  </p>
                </div>
              </div>

              <form onSubmit={handleAddNote} className="flex shrink-0 gap-2 border-t p-4">
                <Input
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder="Add internal note…"
                  aria-label="Add internal note"
                  disabled={noteSaving}
                />
                <Button type="submit" disabled={noteSaving || !note.trim()}>
                  {noteSaving && <Spinner className="size-3.5" />}
                  {noteSaving ? "Adding" : "Add note"}
                </Button>
              </form>
            </>
          )}
        </Card>
      </div>
    </>
  );
}

/** Inline "Saving…" / "Saved" next to the field it belongs to. */
function FieldState({
  field,
  pending,
  saved,
}: {
  field: EditableField;
  pending: EditableField | null;
  saved: EditableField | null;
}) {
  if (pending === field) {
    return (
      <span role="status" className="text-xs font-normal text-muted-foreground">
        Saving…
      </span>
    );
  }
  if (saved === field) {
    return (
      <span role="status" className="text-xs font-normal text-success">
        Saved
      </span>
    );
  }
  return null;
}
