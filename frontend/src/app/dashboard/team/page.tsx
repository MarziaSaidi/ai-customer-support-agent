"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/auth-context";
import { api, type CompanyMember, ApiError } from "@/lib/api";

export default function TeamPage() {
  const { user, profile } = useAuth();
  const [members, setMembers] = useState<CompanyMember[]>([]);
  const [email, setEmail] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    if (!user?.companyId) return;
    api.getCompanyMembers(user.companyId).then(setMembers).catch(console.error);
  }, [user?.companyId]);

  async function handleAddMember(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !isAdmin) return;

    setError("");
    setLoading(true);
    try {
      const member = await api.addCompanyMember(user.companyId, {
        email,
        role: "SUPPORT_AGENT",
      });
      setMembers((prev) => [...prev, member]);
      setEmail("");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to add member");
    } finally {
      setLoading(false);
    }
  }

  async function handleRemove(userId: number) {
    if (!user?.companyId || !isAdmin) return;
    try {
      await api.removeCompanyMember(user.companyId, userId);
      setMembers((prev) => prev.filter((m) => m.userId !== userId));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to remove member");
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold">Team</h1>
      <p className="mt-1 text-muted-foreground">Manage who has access to your company workspace.</p>

      <Card className="mt-8">
        <CardHeader>
          <CardTitle>Members</CardTitle>
          <CardDescription>Admins can invite registered users by email.</CardDescription>
        </CardHeader>
        <CardContent>
          {members.length === 0 ? (
            <p className="text-sm text-muted-foreground">No members found.</p>
          ) : (
            <ul className="space-y-3">
              {members.map((member) => (
                <li key={member.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <p className="font-medium">
                      {member.firstName} {member.lastName}
                    </p>
                    <p className="text-sm text-muted-foreground">{member.email}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">{member.role}</Badge>
                    {isAdmin && member.userId !== profile?.id && (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleRemove(member.userId)}
                        disabled={member.role === "ADMIN" && members.filter((m) => m.role === "ADMIN").length === 1}
                      >
                        Remove
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      {isAdmin && (
        <Card className="mt-6">
          <CardHeader>
            <CardTitle>Add team member</CardTitle>
            <CardDescription>User must already have a SupportIQ account.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleAddMember} className="flex flex-col gap-4 sm:flex-row sm:items-end">
              <div className="flex-1 space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="agent@company.com"
                  required
                />
              </div>
              <Button type="submit" disabled={loading}>
                {loading ? "Adding..." : "Add as Support Agent"}
              </Button>
            </form>
            {error && <p className="mt-3 text-sm text-destructive">{error}</p>}
          </CardContent>
        </Card>
      )}
    </>
  );
}
