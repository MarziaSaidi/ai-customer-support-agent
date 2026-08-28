import { AlertCircleIcon, RefreshCwIcon } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

/**
 * Shown while a request is in flight. Announces politely so a screen-reader
 * user knows the page is working, and explains a long wait rather than
 * leaving an unexplained spinner (the hosted backend cold-starts).
 */
function LoadingNotice({
  label = "Loading",
  slow = false,
  className,
}: {
  label?: string
  slow?: boolean
  className?: string
}) {
  return (
    <div
      role="status"
      aria-live="polite"
      className={cn("flex flex-col gap-1 text-sm text-muted-foreground", className)}
    >
      <span>{label}…</span>
      {slow && (
        <span className="text-xs">
          The server sleeps when idle, so the first request can take up to a minute.
        </span>
      )}
    </div>
  )
}

function ErrorState({
  title = "Couldn't load this",
  description,
  onRetry,
  className,
}: {
  title?: string
  description?: string | null
  onRetry?: () => void
  className?: string
}) {
  return (
    <div
      role="alert"
      className={cn(
        "flex flex-col items-start gap-3 rounded-lg border border-destructive/30 bg-destructive/5 p-4",
        className
      )}
    >
      <div className="flex items-start gap-2">
        <AlertCircleIcon aria-hidden="true" className="mt-0.5 size-4 shrink-0 text-destructive" />
        <div className="space-y-1">
          <p className="text-sm font-medium text-foreground">{title}</p>
          {description && <p className="text-sm text-muted-foreground">{description}</p>}
        </div>
      </div>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCwIcon aria-hidden="true" />
          Try again
        </Button>
      )}
    </div>
  )
}

/**
 * Only ever rendered once we know the request succeeded and returned nothing —
 * never as a stand-in for a pending or failed load.
 */
function EmptyState({
  title,
  description,
  action,
  className,
}: {
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}) {
  return (
    <div className={cn("flex flex-col items-start gap-3 py-2", className)}>
      <div className="space-y-1">
        <p className="text-sm font-medium">{title}</p>
        {description && <p className="text-sm text-muted-foreground">{description}</p>}
      </div>
      {action}
    </div>
  )
}

/** Placeholder rows that match the shape of the list they stand in for. */
function SkeletonRows({ rows = 4, className }: { rows?: number; className?: string }) {
  return (
    <div className={cn("space-y-3", className)} aria-hidden="true">
      {Array.from({ length: rows }).map((_, index) => (
        <div key={index} className="space-y-2">
          <Skeleton className="h-4 w-1/2" />
          <Skeleton className="h-3 w-3/4" />
        </div>
      ))}
    </div>
  )
}

export { LoadingNotice, ErrorState, EmptyState, SkeletonRows }
