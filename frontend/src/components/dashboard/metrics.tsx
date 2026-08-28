import { Skeleton } from "@/components/ui/skeleton"
import { cn } from "@/lib/utils"

export interface Metric {
  label: string
  value: string | number | null
  /** Optional one-line qualifier shown under the value. */
  hint?: string
}

/**
 * One framed strip divided by hairlines rather than four detached cards.
 * The numbers read as a single set, and the card chrome stops competing with
 * the values it contains.
 */
export function MetricGroup({
  metrics,
  loading = false,
  className,
}: {
  metrics: Metric[]
  loading?: boolean
  className?: string
}) {
  return (
    <dl
      className={cn(
        "grid grid-cols-1 overflow-hidden rounded-xl bg-card ring-1 ring-foreground/10",
        "divide-y divide-border sm:grid-cols-2 sm:divide-y-0",
        "[&>*:nth-child(n+3)]:sm:border-t sm:[&>*:nth-child(even)]:border-l",
        "lg:grid-cols-4 lg:divide-y-0 lg:[&>*]:border-l lg:[&>*:first-child]:border-l-0",
        "lg:[&>*:nth-child(n+3)]:border-t-0",
        className
      )}
    >
      {metrics.map((metric) => (
        <div key={metric.label} className="min-w-0 px-5 py-4">
          <dt className="text-eyebrow text-muted-foreground">{metric.label}</dt>
          <dd className="mt-1.5">
            {loading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <span className="block text-3xl leading-none font-semibold tracking-tight tabular-nums">
                {metric.value ?? "—"}
              </span>
            )}
            {metric.hint && !loading && (
              <span className="mt-1 block text-xs text-muted-foreground">{metric.hint}</span>
            )}
          </dd>
        </div>
      ))}
    </dl>
  )
}
