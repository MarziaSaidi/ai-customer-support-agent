"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuth } from "@/contexts/auth-context";
import { api, type Company, ApiError } from "@/lib/api";

export default function SettingsPage() {
  const { user } = useAuth();
  const [company, setCompany] = useState<Company | null>(null);
  const [name, setName] = useState("");
  const [website, setWebsite] = useState("");
  const [aiSystemPrompt, setAiSystemPrompt] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    if (!user?.companyId) return;
    api.getCompany(user.companyId).then((data) => {
      setCompany(data);
      setName(data.name);
      setWebsite(data.website ?? "");
      setAiSystemPrompt(data.aiSystemPrompt ?? "");
    }).catch(console.error);
  }, [user?.companyId]);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !isAdmin) return;

    setError("");
    setMessage("");
    setLoading(true);
    try {
      const updated = await api.updateCompany(user.companyId, {
        name,
        website,
        aiSystemPrompt,
      });
      setCompany(updated);
      setMessage("Settings saved.");
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to save settings");
    } finally {
      setLoading(false);
    }
  }

  if (!isAdmin) {
    return (
      <>
        <h1 className="text-2xl font-bold">Settings</h1>
        <p className="mt-4 text-muted-foreground">Only admins can change company settings.</p>
      </>
    );
  }

  return (
    <>
      <h1 className="text-2xl font-bold">Settings</h1>
      <p className="mt-1 text-muted-foreground">Configure your company workspace and AI behavior.</p>

      <Card className="mt-8">
        <CardHeader>
          <CardTitle>Company profile</CardTitle>
          <CardDescription>
            {company ? `Workspace slug: ${company.slug}` : "Loading..."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSave} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">Company name</Label>
              <Input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="website">Website</Label>
              <Input
                id="website"
                value={website}
                onChange={(e) => setWebsite(e.target.value)}
                placeholder="https://example.com"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="aiSystemPrompt">AI system prompt</Label>
              <textarea
                id="aiSystemPrompt"
                value={aiSystemPrompt}
                onChange={(e) => setAiSystemPrompt(e.target.value)}
                rows={5}
                className="flex w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs outline-none focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50"
              />
            </div>
            {error && <p className="text-sm text-destructive">{error}</p>}
            {message && <p className="text-sm text-green-600">{message}</p>}
            <Button type="submit" disabled={loading}>
              {loading ? "Saving..." : "Save changes"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </>
  );
}
