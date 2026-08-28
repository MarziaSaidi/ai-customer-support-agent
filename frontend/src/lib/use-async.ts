"use client";

import { useEffect, useRef, useState, type DependencyList } from "react";
import { ApiError } from "@/lib/api";

export type AsyncStatus = "idle" | "loading" | "ready" | "error";

/**
 * The hosted backend sleeps when idle, so a cold request can take ~30-50s.
 * After this long we tell the user *why* they are waiting instead of
 * leaving them with an unexplained spinner.
 */
const SLOW_REQUEST_MS = 4000;

export function errorMessage(err: unknown, fallback: string) {
  if (err instanceof ApiError) return err.message;
  // fetch() rejects with a TypeError when the request never reached the server
  // at all. Surfacing the raw "Failed to fetch" tells the user nothing useful —
  // and on the hosted free tier this usually means the backend is asleep.
  if (err instanceof TypeError) {
    return "Can't reach the server. It may be waking up — try again in a moment.";
  }
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}

interface Settled<T> {
  /** Which request this result belongs to. */
  key: string;
  data: T | null;
  error: string | null;
}

/**
 * Loads data and reports the states a page actually has to render: idle (no
 * inputs yet), loading, error with a retry path, and ready.
 *
 * Status is derived during render by comparing the settled result against the
 * current request key, so nothing is set synchronously inside an effect.
 * `deps` must be primitives — they are stringified to form that key.
 */
export function useAsync<T>(fetcher: (() => Promise<T>) | null, deps: DependencyList) {
  const [nonce, setNonce] = useState(0);
  const [settled, setSettled] = useState<Settled<T> | null>(null);
  const [slowKey, setSlowKey] = useState<string | null>(null);

  const requestKey = `${deps.map(String).join("|")}#${nonce}`;

  // Written in an effect (never during render) so the fetch effect below, which
  // is declared after it, always reads the current fetcher.
  const fetcherRef = useRef(fetcher);
  useEffect(() => {
    fetcherRef.current = fetcher;
  });

  useEffect(() => {
    const run = fetcherRef.current;
    if (!run) return;

    let cancelled = false;
    const slowTimer = setTimeout(() => {
      if (!cancelled) setSlowKey(requestKey);
    }, SLOW_REQUEST_MS);

    run()
      .then((data) => {
        if (!cancelled) setSettled({ key: requestKey, data, error: null });
      })
      .catch((err) => {
        if (!cancelled) {
          setSettled({
            key: requestKey,
            data: null,
            error: errorMessage(err, "Could not reach the server."),
          });
        }
      })
      .finally(() => clearTimeout(slowTimer));

    return () => {
      cancelled = true;
      clearTimeout(slowTimer);
    };
  }, [requestKey]);

  const isSettled = settled?.key === requestKey;
  const status: AsyncStatus =
    fetcher === null ? "idle" : !isSettled ? "loading" : settled.error ? "error" : "ready";

  return {
    data: isSettled ? settled.data : null,
    error: isSettled ? settled.error : null,
    status,
    slow: status === "loading" && slowKey === requestKey,
    /** Replace the loaded data locally after a mutation, without refetching. */
    setData: (data: T) => setSettled({ key: requestKey, data, error: null }),
    reload: () => setNonce((n) => n + 1),
  };
}
