import Link from "next/link";
import { ArrowRightIcon } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ThemeToggle } from "@/components/theme/theme-toggle";

const features = [
  {
    title: "AI-powered chat",
    description:
      "Customers get instant answers from your documentation, in natural conversation.",
  },
  {
    title: "Knowledge base",
    description:
      "Upload PDFs, Word docs, FAQs, and markdown — the AI searches them before every reply.",
  },
  {
    title: "Smart actions",
    description:
      "Check orders, process refunds, update addresses, and create tickets automatically.",
  },
  {
    title: "Human escalation",
    description: "Hand off to a support agent the moment the AI can't resolve an issue.",
  },
  {
    title: "Analytics",
    description:
      "Track resolution rates, response times, satisfaction scores, and common questions.",
  },
  {
    title: "Team management",
    description:
      "Role-based access for admins and support agents, with full conversation history.",
  },
];

const pipeline = [
  { step: "Upload", detail: "PDFs, FAQs, and docs" },
  { step: "Index", detail: "Chunked and embedded" },
  { step: "Retrieve", detail: "Similarity search per question" },
  { step: "Answer", detail: "Grounded reply, with sources" },
];

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <header className="sticky top-0 z-30 border-b bg-background/85 backdrop-blur-sm">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6 sm:py-4">
          <Link
            href="/"
            className="flex items-center gap-2 rounded-md outline-none focus-visible:ring-3 focus-visible:ring-ring/50"
          >
            <span
              aria-hidden="true"
              className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-sm font-bold text-primary-foreground"
            >
              IQ
            </span>
            <span className="text-lg font-semibold tracking-tight">SupportIQ</span>
          </Link>
          <nav className="flex items-center gap-2 sm:gap-3">
            <ThemeToggle className="hidden sm:inline-flex" />
            <Link href="/login">
              <Button variant="ghost">Log in</Button>
            </Link>
            <Link href="/register">
              <Button>Get started</Button>
            </Link>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <section className="mx-auto max-w-6xl px-4 pt-16 pb-20 sm:px-6 sm:pt-24 sm:pb-28">
          <div className="grid items-start gap-12 lg:grid-cols-[1.35fr_1fr] lg:gap-16">
            <div className="animate-enter">
              <p className="text-eyebrow text-muted-foreground">RAG-powered support SaaS</p>
              <h1 className="mt-5 max-w-[18ch] text-4xl leading-[1.05] font-semibold tracking-tight text-balance sm:text-5xl lg:text-6xl">
                AI customer support that answers from your documentation
              </h1>
              <p className="mt-6 max-w-[52ch] text-lg text-pretty text-muted-foreground">
                Upload PDFs and FAQs. SupportIQ searches your knowledge base, answers customers,
                creates tickets when needed, and tracks what people ask most.
              </p>
              <div className="mt-9 flex flex-wrap items-center gap-3">
                <Link href="/register">
                  <Button size="lg" className="group h-11 px-5 text-base">
                    Start free trial
                    <ArrowRightIcon
                      aria-hidden="true"
                      className="transition-transform group-hover:translate-x-0.5"
                    />
                  </Button>
                </Link>
                <Link href="/dashboard">
                  <Button size="lg" variant="outline" className="h-11 px-5 text-base">
                    View dashboard
                  </Button>
                </Link>
              </div>
            </div>

            {/* The pipeline is the product's actual mechanism — it earns the
                space better than a decorative visual would. */}
            <div
              className="animate-enter rounded-xl bg-card ring-1 ring-foreground/10"
              style={{ animationDelay: "90ms" }}
            >
              <p className="text-eyebrow border-b px-5 py-3 text-muted-foreground">
                How an answer is made
              </p>
              <ol className="divide-y">
                {pipeline.map((item, index) => (
                  <li key={item.step} className="flex items-baseline gap-4 px-5 py-4">
                    <span className="text-eyebrow w-6 shrink-0 text-muted-foreground tabular-nums">
                      {String(index + 1).padStart(2, "0")}
                    </span>
                    <span className="min-w-0">
                      <span className="block text-sm font-medium">{item.step}</span>
                      <span className="block text-sm text-muted-foreground">{item.detail}</span>
                    </span>
                  </li>
                ))}
              </ol>
            </div>
          </div>
        </section>

        <section className="border-t bg-muted/30">
          <div className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-24">
            <div className="max-w-2xl">
              <p className="text-eyebrow text-muted-foreground">Platform</p>
              <h2 className="mt-4 text-3xl font-semibold tracking-tight text-balance sm:text-4xl">
                Everything you need to run support
              </h2>
            </div>

            {/* A single ruled grid rather than six detached cards — the set reads
                as one system, and the rules do the separating. */}
            <ul className="mt-12 grid overflow-hidden rounded-xl bg-card ring-1 ring-foreground/10 sm:grid-cols-2 lg:grid-cols-3">
              {features.map((feature, index) => (
                <li
                  key={feature.title}
                  className="border-b p-6 last:border-b-0 sm:[&:nth-child(n+5)]:border-b-0 sm:[&:nth-child(even)]:border-l lg:[&:nth-child(n+4)]:border-b-0 lg:[&:nth-child(even)]:border-l-0 lg:[&:not(:nth-child(3n+1))]:border-l"
                >
                  <p className="text-eyebrow text-muted-foreground tabular-nums">
                    {String(index + 1).padStart(2, "0")}
                  </p>
                  <h3 className="mt-3 text-base font-medium">{feature.title}</h3>
                  <p className="mt-2 text-sm text-pretty text-muted-foreground">
                    {feature.description}
                  </p>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-4 py-20 sm:px-6 sm:py-24">
          <div className="flex flex-col items-start gap-8 rounded-xl bg-primary px-6 py-12 text-primary-foreground sm:px-12 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-xl">
              <h2 className="text-2xl font-semibold tracking-tight text-balance sm:text-3xl">
                Ready to transform your support?
              </h2>
              <p className="mt-3 text-primary-foreground/75">
                Set up your company, upload your docs, and go live in minutes.
              </p>
            </div>
            <Link href="/register" className="shrink-0">
              <Button size="lg" variant="secondary" className="group h-11 px-5 text-base">
                Create your account
                <ArrowRightIcon
                  aria-hidden="true"
                  className="transition-transform group-hover:translate-x-0.5"
                />
              </Button>
            </Link>
          </div>
        </section>
      </main>

      <footer className="border-t">
        <div className="mx-auto flex max-w-6xl flex-col gap-3 px-4 py-8 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-6">
          <p>SupportIQ — AI-powered customer support platform</p>
          <div className="flex items-center gap-4">
            <Link
              href="/login"
              className="rounded-md underline-offset-4 outline-none hover:text-foreground hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              Log in
            </Link>
            <Link
              href="/register"
              className="rounded-md underline-offset-4 outline-none hover:text-foreground hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              Get started
            </Link>
            <ThemeToggle className="sm:hidden" />
          </div>
        </div>
      </footer>
    </div>
  );
}
