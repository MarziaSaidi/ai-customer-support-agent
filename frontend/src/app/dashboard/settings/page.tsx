"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Spinner } from "@/components/ui/spinner";
import { ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { useAuth } from "@/contexts/auth-context";
import { api } from "@/lib/api";
import { errorMessage, useAsync } from "@/lib/use-async";

export default function SettingsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const isAdmin = user?.role === "ADMIN";

  const company = useAsync(
    companyId && isAdmin ? () => api.getCompany(companyId) : null,
    [companyId, isAdmin]
  );

  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function handleSave(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!companyId || !isAdmin) return;

    const form = new FormData(e.currentTarget);
    setError("");
    setNotice("");
    setSaving(true);
    try {
      company.setData(
        await api.updateCompany(companyId, {
          name: form.get("name") as string,
          website: form.get("website") as string,
          aiSystemPrompt: form.get("aiSystemPrompt") as string,
        })
      );
      setNotice("Settings saved.");
    } catch (err) {
      setError(errorMessage(err, "Failed to save settings"));
    } finally {
      setSaving(false);
    }
  }

  if (!isAdmin) {
    return (
      <>
        <PageHeader title="Settings" description="Company workspace and AI behavior." />
        <Card>
          <CardContent className="py-2">
            <p className="text-sm text-muted-foreground">
              Only admins can change company settings. Ask an admin on your team if something
              here needs to change.
            </p>
          </CardContent>
        </Card>
      </>
    );
  }

  const data = company.data;

  return (
    <>
      <PageHeader
        title="Settings"
        description="Configure your company workspace and how the AI responds."
      />

      <Card>
        <CardHeader>
          <CardTitle>Company profile</CardTitle>
          <CardDescription>{data ? `Workspace slug: ${data.slug}` : " "}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {company.status !== "ready" && company.status !== "error" && (
            <>
              <SkeletonRows rows={3} />
              <LoadingNotice label="Loading settings" slow={company.slow} />
            </>
          )}

          {company.status === "error" && (
            <ErrorState description={company.error} onRetry={company.reload} />
          )}

          {company.status === "ready" && data && (
            /* Uncontrolled, keyed by the record it came from: the fields adopt
               fresh values whenever the company reloads, with no sync effect. */
            <form key={data.id} onSubmit={handleSave} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="name">Company name</Label>
                <Input
                  id="name"
                  name="name"
                  defaultValue={data.name}
                  autoComplete="organization"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="website">
                  Website <span className="text-muted-foreground">(optional)</span>
                </Label>
                <Input
                  id="website"
                  name="website"
                  type="url"
                  autoComplete="url"
                  defaultValue={data.website ?? ""}
                  placeholder="https://example.com"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="aiSystemPrompt">AI system prompt</Label>
                <Textarea
                  id="aiSystemPrompt"
                  name="aiSystemPrompt"
                  defaultValue={data.aiSystemPrompt ?? ""}
                  rows={5}
                  aria-describedby="aiSystemPrompt-hint"
                  placeholder="You are a support agent for Acme. Be concise and never invent policy."
                />
                <p id="aiSystemPrompt-hint" className="text-xs text-muted-foreground">
                  Sets the AI&apos;s tone and boundaries on every customer reply. It still answers
                  only from your uploaded documents.
                </p>
              </div>

              {error && (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              )}
              {notice && (
                <p role="status" className="text-sm text-success">
                  {notice}
                </p>
              )}

              <Button type="submit" disabled={saving}>
                {saving && <Spinner className="size-3.5" />}
                {saving ? "Saving" : "Save changes"}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </>
  );
}
