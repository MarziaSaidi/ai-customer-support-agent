import { Loader2Icon } from "lucide-react"

import { cn } from "@/lib/utils"

/**
 * Decorative only — the surrounding element carries the accessible name and
 * the live announcement, so the icon itself stays hidden from assistive tech.
 */
function Spinner({ className, ...props }: React.ComponentProps<typeof Loader2Icon>) {
  return (
    <Loader2Icon
      aria-hidden="true"
      className={cn("size-4 animate-spin motion-reduce:animate-none", className)}
      {...props}
    />
  )
}

export { Spinner }
