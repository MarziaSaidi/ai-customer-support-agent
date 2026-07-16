import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";

const features = [
  {
    title: "AI-Powered Chat",
    description: "Customers get instant answers from your documentation with natural conversation.",
  },
  {
    title: "Knowledge Base",
    description: "Upload PDFs, Word docs, FAQs, and markdown — the AI searches them before every reply.",
  },
  {
    title: "Smart Actions",
    description: "Check orders, process refunds, update addresses, and create tickets automatically.",
  },
  {
    title: "Human Escalation",
    description: "Seamlessly hand off to support agents when the AI can't resolve an issue.",
  },
  {
    title: "Analytics",
    description: "Track resolution rates, response times, satisfaction scores, and common questions.",
  },
  {
    title: "Team Management",
    description: "Role-based access for admins and support agents with full conversation history.",
  },
];

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <header className="border-b">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground text-sm font-bold">
              IQ
            </div>
            <span className="text-lg font-semibold">SupportIQ</span>
          </div>
          <nav className="flex items-center gap-3">
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
        <section className="mx-auto max-w-6xl px-6 py-20 text-center">
          <Badge variant="secondary" className="mb-4">
            RAG-Powered Support SaaS
          </Badge>
          <h1 className="mx-auto max-w-3xl text-4xl font-bold tracking-tight sm:text-5xl">
            AI customer support that answers from your documentation
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg text-muted-foreground">
            Upload PDFs and FAQs. SupportIQ searches your knowledge base, answers customers,
            creates tickets when needed, and tracks what people ask most.
          </p>
          <div className="mt-8 flex justify-center gap-4">
            <Link href="/register">
              <Button size="lg">Start free trial</Button>
            </Link>
            <Link href="/dashboard">
              <Button size="lg" variant="outline">
                View dashboard
              </Button>
            </Link>
          </div>
        </section>

        <section className="border-t bg-muted/30 py-20">
          <div className="mx-auto max-w-6xl px-6">
            <h2 className="mb-12 text-center text-2xl font-semibold">Everything you need</h2>
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {features.map((feature) => (
                <Card key={feature.title}>
                  <CardHeader>
                    <CardTitle className="text-lg">{feature.title}</CardTitle>
                    <CardDescription>{feature.description}</CardDescription>
                  </CardHeader>
                  <CardContent />
                </Card>
              ))}
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-6 py-20">
          <Card className="bg-primary text-primary-foreground">
            <CardHeader className="text-center">
              <CardTitle className="text-2xl">Ready to transform your support?</CardTitle>
              <CardDescription className="text-primary-foreground/80">
                Set up your company, upload docs, and go live in minutes.
              </CardDescription>
            </CardHeader>
            <CardContent className="flex justify-center pb-8">
              <Link href="/register">
                <Button size="lg" variant="secondary">
                  Create your account
                </Button>
              </Link>
            </CardContent>
          </Card>
        </section>
      </main>

      <footer className="border-t py-8 text-center text-sm text-muted-foreground">
        SupportIQ &mdash; AI-Powered Customer Support Platform
      </footer>
    </div>
  );
}
