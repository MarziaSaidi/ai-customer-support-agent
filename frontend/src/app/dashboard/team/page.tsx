"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { EmptyState, ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { useAuth } from "@/contexts/auth-context";
import { api } from "@/lib/api";
import { errorMessage, useAsync } from "@/lib/use-async";

function humanizeRole(role: string) {
  return role.charAt(0) + role.slice(1).toLowerCase().replace(/_/g, " ");
}

export default function TeamPage() {
  const { user, profile } = useAuth();
  const companyId = user?.companyId ?? null;
  const isAdmin = user?.role === "ADMIN";

  const members = useAsync(
    companyId ? () => api.getCompanyMembers(companyId) : null,
    [companyId]
  );

  const [email, setEmail] = useState("");
  const [addError, setAddError] = useState("");
  const [addNotice, setAddNotice] = useState("");
  const [adding, setAdding] = useState(false);

  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [removingId, setRemovingId] = useState<number | null>(null);
  const [removeError, setRemoveError] = useState("");

  const list = members.data ?? [];
  const adminCount = list.filter((m) => m.role === "ADMIN").length;

  async function handleAddMember(e: React.FormEvent) {
    e.preventDefault();
    if (!companyId || !isAdmin) return;

    setAddError("");
    setAddNotice("");
    setAdding(true);
    try {
      const member = await api.addCompanyMember(companyId, { email, role: "SUPPORT_AGENT" });
      members.setData([...list, member]);
      setAddNotice(`${member.firstName} ${member.lastName} was added as a support agent.`);
      setEmail("");
    } catch (err) {
      setAddError(errorMessage(err, "Failed to add member"));
    } finally {
      setAdding(false);
    }
  }

  async function handleRemove(userId: number) {
    if (!companyId || !isAdmin) return;

    setRemovingId(userId);
    setRemoveError("");
    try {
      await api.removeCompanyMember(companyId, userId);
      members.setData(list.filter((m) => m.userId !== userId));
      setConfirmingId(null);
    } catch (err) {
      setRemoveError(errorMessage(err, "Failed to remove member"));
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <>
      <PageHeader
        title="Team"
        description="Manage who has access to your company workspace."
      />

      <Card>
        <CardHeader>
          <CardTitle>Members</CardTitle>
          <CardDescription>Admins can invite people who already have an account.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {members.status === "loading" && (
            <>
              <SkeletonRows rows={3} />
              <LoadingNotice label="Loading team" slow={members.slow} />
            </>
          )}

          {members.status === "error" && (
            <ErrorState description={members.error} onRetry={members.reload} />
          )}

          {members.status === "ready" && list.length === 0 && (
            <EmptyState title="No members found" description="Invite a teammate below." />
          )}

          {removeError && <ErrorState title="Couldn't remove member" description={removeError} />}

          {members.status === "ready" && list.length > 0 && (
            <ul className="space-y-3">
              {list.map((member) => {
                const isLastAdmin = member.role === "ADMIN" && adminCount === 1;
                const isSelf = member.userId === profile?.id;
                return (
                  <li key={member.id} className="rounded-lg border p-3">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="min-w-0">
                        <p className="truncate text-sm font-medium">
                          {member.firstName} {member.lastName}
                          {isSelf && <span className="text-muted-foreground"> · you</span>}
                        </p>
                        <p className="truncate text-sm text-muted-foreground">{member.email}</p>
                      </div>
                      <div className="flex shrink-0 items-center gap-2">
                        <Badge variant={member.role === "ADMIN" ? "default" : "secondary"}>
                          {humanizeRole(member.role)}
                        </Badge>
                        {isAdmin && !isSelf && confirmingId !== member.userId && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setConfirmingId(member.userId)}
                            disabled={isLastAdmin}
                            title={
                              isLastAdmin
                                ? "A workspace needs at least one admin"
                                : undefined
                            }
                          >
                            Remove
                          </Button>
                        )}
                      </div>
                    </div>

                    {isAdmin && !isSelf && isLastAdmin && (
                      <p className="mt-2 text-xs text-muted-foreground">
                        This is the only admin — promote someone else before removing them.
                      </p>
                    )}

                    {confirmingId === member.userId && (
                      <div
                        role="alertdialog"
                        aria-label={`Remove ${member.firstName} ${member.lastName}`}
                        className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-md border border-destructive/30 bg-destructive/5 p-3"
                      >
                        <p className="text-sm">
                          Remove {member.firstName} from this workspace?
                        </p>
                        <div className="flex gap-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setConfirmingId(null)}
                            disabled={removingId === member.userId}
                          >
                            Cancel
                          </Button>
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleRemove(member.userId)}
                            disabled={removingId === member.userId}
                          >
                            {removingId === member.userId && <Spinner className="size-3.5" />}
                            {removingId === member.userId ? "Removing" : "Remove"}
                          </Button>
                        </div>
                      </div>
                    )}
                  </li>
                );
              })}
            </ul>
          )}
        </CardContent>
      </Card>

      {isAdmin && (
        <Card>
          <CardHeader>
            <CardTitle>Add team member</CardTitle>
            <CardDescription>They must already have a SupportIQ account.</CardDescription>
          </CardHeader>
          <CardContent className="space-y-3">
            <form onSubmit={handleAddMember} className="flex flex-col gap-4 sm:flex-row sm:items-end">
              <div className="flex-1 space-y-2">
                <Label htmlFor="member-email">Email</Label>
                <Input
                  id="member-email"
                  type="email"
                  autoComplete="off"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="agent@company.com"
                  aria-invalid={addError ? true : undefined}
                  required
                />
              </div>
              <Button type="submit" disabled={adding || !email.trim()}>
                {adding && <Spinner className="size-3.5" />}
                {adding ? "Adding" : "Add as support agent"}
              </Button>
            </form>
            {addError && (
              <p role="alert" className="text-sm text-destructive">
                {addError}
              </p>
            )}
            {addNotice && (
              <p role="status" className="text-sm text-success">
                {addNotice}
              </p>
            )}
          </CardContent>
        </Card>
      )}
    </>
  );
}
