"use client";

import { useState } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { GuestRoute } from "@/components/auth/guest-route";
import { useAuth } from "@/contexts/auth-context";
import { ApiError } from "@/lib/api";

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
      if (err instanceof ApiError) {
        setError(err.message);
        if (err.errors) {
          setFieldErrors(err.errors);
        }
      } else {
        setError("Registration failed");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <GuestRoute>
      <div className="flex flex-1 items-center justify-center px-4 py-12">
        <Card className="w-full max-w-md">
          <CardHeader>
            <CardTitle>Create your account</CardTitle>
            <CardDescription>Set up your company and start automating support.</CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="firstName">First name</Label>
                  <Input id="firstName" name="firstName" required />
                  {fieldErrors.firstName && (
                    <p className="text-xs text-destructive">{fieldErrors.firstName}</p>
                  )}
                </div>
                <div className="space-y-2">
                  <Label htmlFor="lastName">Last name</Label>
                  <Input id="lastName" name="lastName" required />
                  {fieldErrors.lastName && (
                    <p className="text-xs text-destructive">{fieldErrors.lastName}</p>
                  )}
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="companyName">Company name</Label>
                <Input id="companyName" name="companyName" required />
                {fieldErrors.companyName && (
                  <p className="text-xs text-destructive">{fieldErrors.companyName}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input id="email" name="email" type="email" autoComplete="email" required />
                {fieldErrors.email && (
                  <p className="text-xs text-destructive">{fieldErrors.email}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="password">Password</Label>
                <Input id="password" name="password" type="password" minLength={8} autoComplete="new-password" required />
                {fieldErrors.password && (
                  <p className="text-xs text-destructive">{fieldErrors.password}</p>
                )}
              </div>
              {error && <p className="text-sm text-destructive">{error}</p>}
              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? "Creating account..." : "Create account"}
              </Button>
            </form>
            <p className="mt-4 text-center text-sm text-muted-foreground">
              Already have an account?{" "}
              <Link href="/login" className="text-primary hover:underline">
                Log in
              </Link>
            </p>
          </CardContent>
        </Card>
      </div>
    </GuestRoute>
  );
}
