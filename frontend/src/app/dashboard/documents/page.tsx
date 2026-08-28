"use client";

import { useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Spinner } from "@/components/ui/spinner";
import { EmptyState, ErrorState, LoadingNotice, SkeletonRows } from "@/components/ui/state";
import { PageHeader } from "@/components/dashboard/page-header";
import { useAuth } from "@/contexts/auth-context";
import { api, type DocumentChunkMatch, type RagAnswer } from "@/lib/api";
import { errorMessage, useAsync } from "@/lib/use-async";

/** Stop polling after ~2 minutes rather than retrying a stuck job forever. */
const MAX_POLLS = 40;
const POLL_INTERVAL_MS = 3000;

export default function DocumentsPage() {
  const { user } = useAuth();
  const companyId = user?.companyId ?? null;
  const isAdmin = user?.role === "ADMIN";

  const documents = useAsync(companyId ? () => api.getDocuments(companyId) : null, [companyId]);

  const [title, setTitle] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [uploadError, setUploadError] = useState("");
  const [uploadNotice, setUploadNotice] = useState("");
  const [uploading, setUploading] = useState(false);

  const [confirmingId, setConfirmingId] = useState<number | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteError, setDeleteError] = useState("");

  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<DocumentChunkMatch[] | null>(null);
  const [searchError, setSearchError] = useState("");
  const [searchLoading, setSearchLoading] = useState(false);

  const [askQuestion, setAskQuestion] = useState("");
  const [askAnswer, setAskAnswer] = useState<RagAnswer | null>(null);
  const [askError, setAskError] = useState("");
  const [askLoading, setAskLoading] = useState(false);

  const docs = documents.data ?? [];
  const pendingCount = docs.filter((doc) => !doc.processed).length;
  const pollsRef = useRef(0);
  const [gaveUpPolling, setGaveUpPolling] = useState(false);

  // Only meaningful while something is still pending, so it needs no reset.
  const pollingStalled = gaveUpPolling && pendingCount > 0;

  // While a document is still being embedded, refresh so its status flips on
  // its own — but give up rather than polling a stuck job indefinitely.
  useEffect(() => {
    if (!companyId || pendingCount === 0) return;

    pollsRef.current = 0;
    const interval = setInterval(() => {
      if (pollsRef.current >= MAX_POLLS) {
        clearInterval(interval);
        setGaveUpPolling(true);
        return;
      }
      pollsRef.current += 1;
      api.getDocuments(companyId).then(documents.setData).catch(() => {});
    }, POLL_INTERVAL_MS);

    return () => clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [companyId, pendingCount]);

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!companyId || !file || !isAdmin) return;

    setUploadError("");
    setUploadNotice("");
    setGaveUpPolling(false);
    setUploading(true);
    try {
      const doc = await api.uploadDocument(companyId, title, inferDocumentType(file.name), file);
      documents.setData([doc, ...docs]);
      setUploadNotice(`“${doc.title}” uploaded. Processing starts automatically.`);
      setTitle("");
      setFile(null);
      (e.target as HTMLFormElement).reset();
    } catch (err) {
      setUploadError(errorMessage(err, "Upload failed"));
    } finally {
      setUploading(false);
    }
  }

  async function handleDelete(id: number) {
    if (!isAdmin) return;
    setDeletingId(id);
    setDeleteError("");
    try {
      await api.deleteDocument(id);
      documents.setData(docs.filter((d) => d.id !== id));
      setConfirmingId(null);
    } catch (err) {
      setDeleteError(errorMessage(err, "Delete failed"));
    } finally {
      setDeletingId(null);
    }
  }

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!companyId || !searchQuery.trim()) return;

    setSearchLoading(true);
    setSearchError("");
    try {
      setSearchResults(await api.searchDocuments(companyId, searchQuery.trim()));
    } catch (err) {
      setSearchError(errorMessage(err, "Search failed"));
      setSearchResults(null);
    } finally {
      setSearchLoading(false);
    }
  }

  async function handleAsk(e: React.FormEvent) {
    e.preventDefault();
    if (!companyId || !askQuestion.trim()) return;

    setAskLoading(true);
    setAskError("");
    try {
      setAskAnswer(await api.askKnowledge(companyId, askQuestion.trim()));
    } catch (err) {
      setAskError(errorMessage(err, "Ask failed"));
      setAskAnswer(null);
    } finally {
      setAskLoading(false);
    }
  }

  return (
    <>
      <PageHeader
        title="Knowledge base"
        description="Upload the documentation the AI searches before answering customers."
      />

      {isAdmin && (
        <Card>
          <CardHeader>
            <CardTitle>Upload document</CardTitle>
            <CardDescription>PDF, Markdown, Word, or plain text files.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleUpload} className="space-y-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="title">Title</Label>
                  <Input
                    id="title"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    placeholder="Return policy"
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
              </div>
              {uploadError && (
                <p role="alert" className="text-sm text-destructive">
                  {uploadError}
                </p>
              )}
              {uploadNotice && (
                <p role="status" className="text-sm text-success">
                  {uploadNotice}
                </p>
              )}
              <Button type="submit" disabled={uploading || !file}>
                {uploading && <Spinner className="size-3.5" />}
                {uploading ? "Uploading" : "Upload"}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle>Test knowledge search</CardTitle>
          <CardDescription>
            Search processed documents to preview what the AI will retrieve.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={handleSearch} className="flex gap-2">
            <Input
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="How do refunds work?"
              aria-label="Search your knowledge base"
              required
            />
            <Button type="submit" disabled={searchLoading}>
              {searchLoading && <Spinner className="size-3.5" />}
              {searchLoading ? "Searching" : "Search"}
            </Button>
          </form>

          {searchError && <ErrorState title="Search failed" description={searchError} />}

          {searchResults !== null && !searchError && searchResults.length === 0 && (
            <EmptyState
              title="No matching passages"
              description="Try different wording, or upload a document that covers this."
            />
          )}

          {searchResults && searchResults.length > 0 && (
            <ul className="space-y-3">
              {searchResults.map((result) => (
                <li key={result.chunkId} className="rounded-lg border p-3">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-medium">{result.documentTitle}</p>
                    <Badge variant="outline" className="tabular-nums">
                      {result.score.toFixed(2)}
                    </Badge>
                  </div>
                  <p className="mt-2 line-clamp-3 text-sm text-muted-foreground">
                    {result.content}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Ask AI</CardTitle>
          <CardDescription>
            Preview the answer customers will get from your knowledge base.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={handleAsk} className="flex gap-2">
            <Input
              value={askQuestion}
              onChange={(e) => setAskQuestion(e.target.value)}
              placeholder="Can I return shoes after 30 days?"
              aria-label="Ask a question"
              required
            />
            <Button type="submit" disabled={askLoading}>
              {askLoading && <Spinner className="size-3.5" />}
              {askLoading ? "Thinking" : "Ask"}
            </Button>
          </form>

          {askLoading && <LoadingNotice label="Searching your documents and drafting an answer" />}

          {askError && <ErrorState title="Couldn't answer that" description={askError} />}

          {askAnswer && !askLoading && (
            <div className="space-y-3 rounded-lg border p-4">
              <p className="text-sm whitespace-pre-wrap">{askAnswer.answer}</p>
              {askAnswer.sources.length > 0 && (
                <div className="space-y-2">
                  <h3 className="text-eyebrow text-muted-foreground">Sources</h3>
                  {askAnswer.sources.map((source) => (
                    <div
                      key={`${source.documentId}-${source.excerpt}`}
                      className="rounded-md bg-muted/50 p-2.5"
                    >
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

      <Card>
        <CardHeader>
          <CardTitle>Documents</CardTitle>
          <CardDescription>
            {documents.status === "ready"
              ? docs.length === 0
                ? "Nothing uploaded yet."
                : `${docs.length} document${docs.length === 1 ? "" : "s"} in your knowledge base.`
              : " "}
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {documents.status === "loading" && (
            <>
              <SkeletonRows rows={3} />
              <LoadingNotice label="Loading documents" slow={documents.slow} />
            </>
          )}

          {documents.status === "error" && (
            <ErrorState description={documents.error} onRetry={documents.reload} />
          )}

          {documents.status === "ready" && docs.length === 0 && (
            <EmptyState
              title="No documents yet"
              description={
                isAdmin
                  ? "Upload a PDF or FAQ above — the AI can only answer from what it has read."
                  : "Ask an admin to upload your company documentation."
              }
            />
          )}

          {deleteError && <ErrorState title="Delete failed" description={deleteError} />}

          {pollingStalled && (
            <ErrorState
              title="Still processing"
              description="This is taking longer than expected. Reload to check again."
              onRetry={documents.reload}
            />
          )}

          {documents.status === "ready" && docs.length > 0 && (
            <ul className="space-y-3">
              {docs.map((doc) => (
                <li key={doc.id} className="rounded-lg border p-3">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{doc.title}</p>
                      <p className="truncate text-sm text-muted-foreground">{doc.filename}</p>
                    </div>
                    <div className="flex shrink-0 items-center gap-2">
                      <Badge variant="outline">{doc.type}</Badge>
                      <Badge variant={doc.processed ? "success" : "warning"}>
                        {doc.processed ? "Processed" : "Processing"}
                      </Badge>
                      {isAdmin && confirmingId !== doc.id && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setConfirmingId(doc.id)}
                        >
                          Delete
                        </Button>
                      )}
                    </div>
                  </div>

                  {/* Deleting removes the embeddings too, so it asks first. */}
                  {confirmingId === doc.id && (
                    <div
                      role="alertdialog"
                      aria-label={`Delete ${doc.title}`}
                      className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded-md border border-destructive/30 bg-destructive/5 p-3"
                    >
                      <p className="text-sm">
                        Delete “{doc.title}”? The AI will stop answering from it.
                      </p>
                      <div className="flex gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setConfirmingId(null)}
                          disabled={deletingId === doc.id}
                        >
                          Cancel
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleDelete(doc.id)}
                          disabled={deletingId === doc.id}
                        >
                          {deletingId === doc.id && <Spinner className="size-3.5" />}
                          {deletingId === doc.id ? "Deleting" : "Delete"}
                        </Button>
                      </div>
                    </div>
                  )}
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
