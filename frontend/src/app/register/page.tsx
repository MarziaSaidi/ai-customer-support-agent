"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Spinner } from "@/components/ui/spinner";
import { AuthShell } from "@/components/auth/auth-shell";
import { GuestRoute } from "@/components/auth/guest-route";
import { useAuth } from "@/contexts/auth-context";
import { ApiError } from "@/lib/api";
import { errorMessage } from "@/lib/use-async";

export default function RegisterPage() {
  const { register } = useAuth();
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setError("");
    setFieldErrors({});
    setLoading(true);

    const form = new FormData(e.currentTarget);
    try {
      await register({
        email: form.get("email") as string,
        password: form.get("password") as string,
        firstName: form.get("firstName") as string,
        lastName: form.get("lastName") as string,
        companyName: form.get("companyName") as string,
      });
    } catch (err) {
      setError(errorMessage(err, "Registration failed"));
      if (err instanceof ApiError && err.errors) setFieldErrors(err.errors);
    } finally {
      setLoading(false);
    }
  }

  return (
    <GuestRoute>
      <AuthShell
        title="Create your account"
        description="Set up your company and start automating support."
        footer={
          <>
            Already have an account?{" "}
            <Link
              href="/login"
              className="rounded-md font-medium text-foreground underline-offset-4 outline-none hover:underline focus-visible:ring-3 focus-visible:ring-ring/50"
            >
              Log in
            </Link>
          </>
        }
      >
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="firstName">First name</Label>
              <Input
                id="firstName"
                name="firstName"
                autoComplete="given-name"
                required
                {...fieldProps(fieldErrors, "firstName")}
              />
              <FieldError errors={fieldErrors} name="firstName" />
            </div>
            <div className="space-y-2">
              <Label htmlFor="lastName">Last name</Label>
              <Input
                id="lastName"
                name="lastName"
                autoComplete="family-name"
                required
                {...fieldProps(fieldErrors, "lastName")}
              />
              <FieldError errors={fieldErrors} name="lastName" />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="companyName">Company name</Label>
            <Input
              id="companyName"
              name="companyName"
              autoComplete="organization"
              required
              {...fieldProps(fieldErrors, "companyName")}
            />
            <FieldError errors={fieldErrors} name="companyName" />
          </div>

          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              required
              {...fieldProps(fieldErrors, "email")}
            />
            <FieldError errors={fieldErrors} name="email" />
          </div>

          <div className="space-y-2">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              name="password"
              type="password"
              minLength={8}
              autoComplete="new-password"
              aria-describedby={fieldErrors.password ? "password-error" : "password-hint"}
              aria-invalid={fieldErrors.password ? true : undefined}
              required
            />
            {fieldErrors.password ? (
              <FieldError errors={fieldErrors} name="password" />
            ) : (
              <p id="password-hint" className="text-xs text-muted-foreground">
                At least 8 characters.
              </p>
            )}
          </div>

          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}

          <Button type="submit" size="lg" className="w-full" disabled={loading}>
            {loading && <Spinner className="size-3.5" />}
            {loading ? "Creating account" : "Create account"}
          </Button>
        </form>
      </AuthShell>
    </GuestRoute>
  );
}

/** Ties each message to its input so screen readers announce it in context. */
function fieldProps(errors: Record<string, string>, name: string) {
  return {
    "aria-invalid": errors[name] ? (true as const) : undefined,
    "aria-describedby": errors[name] ? `${name}-error` : undefined,
  };
}

function FieldError({ errors, name }: { errors: Record<string, string>; name: string }) {
  if (!errors[name]) return null;
  return (
    <p id={`${name}-error`} className="text-xs text-destructive">
      {errors[name]}
    </p>
  );
}
