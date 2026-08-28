import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** JS-driven scrolling has to opt out of motion explicitly; CSS can't do it. */
export function prefersReducedMotion() {
  if (typeof window === "undefined") return false
  return window.matchMedia("(prefers-reduced-motion: reduce)").matches
}

export function scrollToBottom(element: HTMLElement | null) {
  element?.scrollIntoView({ behavior: prefersReducedMotion() ? "auto" : "smooth" })
}
