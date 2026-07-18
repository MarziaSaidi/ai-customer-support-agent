"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/contexts/auth-context";
import { api, type DocumentItem, type DocumentChunkMatch, type RagAnswer, ApiError } from "@/lib/api";

export default function DocumentsPage() {
  const { user } = useAuth();
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<DocumentChunkMatch[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [askQuestion, setAskQuestion] = useState("");
  const [askAnswer, setAskAnswer] = useState<RagAnswer | null>(null);
  const [askLoading, setAskLoading] = useState(false);
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    if (!user?.companyId) return;
    api.getDocuments(user.companyId).then(setDocuments).catch(console.error);
  }, [user?.companyId]);

  const hasPending = documents.some((doc) => !doc.processed);

  useEffect(() => {
    if (!user?.companyId || !hasPending) return;

    const interval = setInterval(() => {
      api.getDocuments(user.companyId!).then(setDocuments).catch(console.error);
    }, 3000);

    return () => clearInterval(interval);
  }, [user?.companyId, hasPending]);

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !file || !isAdmin) return;

    setError("");
    setLoading(true);
    try {
      const type = inferDocumentType(file.name);
      const doc = await api.uploadDocument(user.companyId, title, type, file);
      setDocuments((prev) => [doc, ...prev]);
      setTitle("");
      setFile(null);
      (e.target as HTMLFormElement).reset();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed");
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    if (!isAdmin) return;
    try {
      await api.deleteDocument(id);
      setDocuments((prev) => prev.filter((d) => d.id !== id));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Delete failed");
    }
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !searchQuery.trim()) return;

    setSearchLoading(true);
    setError("");
    try {
      const results = await api.searchDocuments(user.companyId, searchQuery.trim());
      setSearchResults(results);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Search failed");
      setSearchResults([]);
    } finally {
      setSearchLoading(false);
    }
  }

  async function handleAsk(e: React.FormEvent) {
    e.preventDefault();
    if (!user?.companyId || !askQuestion.trim()) return;

    setAskLoading(true);
    setError("");
    try {
      const answer = await api.askKnowledge(user.companyId, askQuestion.trim());
      setAskAnswer(answer);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Ask failed");
      setAskAnswer(null);
    } finally {
      setAskLoading(false);
    }
  }

  return (
    <>
      <h1 className="text-2xl font-bold">Knowledge Base</h1>
      <p className="mt-1 text-muted-foreground">
        Upload documentation the AI will search before answering customers.
      </p>

      {isAdmin && (
        <Card className="mt-8">
          <CardHeader>
            <CardTitle>Upload document</CardTitle>
            <CardDescription>PDF, Markdown, Word, or plain text files.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleUpload} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="title">Title</Label>
                <Input
                  id="title"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="Return Policy"
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="file">File</Label>
                <Input
                  id="file"
                  type="file"
                  accept=".pdf,.md,.markdown,.txt,.doc,.docx"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                  required
                />
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button type="submit" disabled={loading || !file}>
                {loading ? "Uploading..." : "Upload"}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Test knowledge search</CardTitle>
          <CardDescription>
            Search processed documents to preview what the AI will retrieve.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSearch} className="flex gap-2">
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="How do refunds work?"
              required
            />
            <Button type="submit" disabled={searchLoading}>
              {searchLoading ? "Searching..." : "Search"}
            </Button>
          </form>
          {searchResults.length > 0 && (
            <ul className="mt-4 space-y-3">
              {searchResults.map((result) => (
                <li key={result.chunkId} className="rounded-lg border p-3">
                  <div className="flex items-center justify-between gap-2">
                    <p className="font-medium">{result.documentTitle}</p>
                    <Badge variant="outline">Score {result.score.toFixed(2)}</Badge>
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground line-clamp-3">{result.content}</p>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Ask AI</CardTitle>
          <CardDescription>
            Preview the RAG answer customers will get from your knowledge base.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleAsk} className="flex gap-2">
            <Input
              value={askQuestion}
              onChange={(e) => setAskQuestion(e.target.value)}
              placeholder="Can I return shoes after 30 days?"
              required
            />
            <Button type="submit" disabled={askLoading}>
              {askLoading ? "Thinking..." : "Ask"}
            </Button>
          </form>
          {askAnswer && (
            <div className="mt-4 space-y-3 rounded-lg border p-4">
              <p className="text-sm whitespace-pre-wrap">{askAnswer.answer}</p>
              {askAnswer.sources.length > 0 && (
                <div className="space-y-2">
                  <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">Sources</p>
                  {askAnswer.sources.map((source) => (
                    <div key={`${source.documentId}-${source.excerpt}`} className="rounded-md bg-muted/50 p-2">
                      <p className="text-sm font-medium">{source.documentTitle}</p>
                      <p className="text-sm text-muted-foreground">{source.excerpt}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <Card className="mt-6">
        <CardHeader>
          <CardTitle>Documents</CardTitle>
          <CardDescription>
            {documents.length === 0
              ? "No documents uploaded yet."
              : `${documents.length} document(s) in your knowledge base.`}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {documents.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              {isAdmin
                ? "Upload a PDF or FAQ to get started."
                : "Ask an admin to upload company documentation."}
            </p>
          ) : (
            <ul className="space-y-3">
              {documents.map((doc) => (
                <li key={doc.id} className="flex items-center justify-between rounded-lg border p-3">
                  <div>
                    <p className="font-medium">{doc.title}</p>
                    <p className="text-sm text-muted-foreground">{doc.filename}</p>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant="secondary">{doc.type}</Badge>
                    <Badge variant={doc.processed ? "default" : "outline"}>
                      {doc.processed ? "Processed" : "Pending"}
                    </Badge>
                    {isAdmin && (
                      <Button variant="ghost" size="sm" onClick={() => handleDelete(doc.id)}>
                        Delete
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </>
  );
}

function inferDocumentType(filename: string): string {
  const lower = filename.toLowerCase();
  if (lower.endsWith(".pdf")) return "PDF";
  if (lower.endsWith(".md") || lower.endsWith(".markdown")) return "MARKDOWN";
  if (lower.endsWith(".doc") || lower.endsWith(".docx")) return "WORD";
  if (lower.endsWith(".txt")) return "FAQ";
  return "PDF";
}
