import { useState } from "react";
import { Link } from "react-router-dom";
import { BookOpen, ArrowRight } from "lucide-react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { send, errorMessage } from "../../shared/api";
const schema = z.object({
  email: z.email("Enter a valid email address"),
  password: z.string(),
});
export function AuthPage({
  page,
  onLogin,
}: {
  page: string;
  onLogin: () => void;
}) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const [token] = useState(
    () => new URLSearchParams(location.hash.slice(1)).get("token") ?? "",
  );
  const reset = page === "reset-password",
    verify = page === "verify-email",
    register = page === "register",
    forgot = page === "forgot-password";
  const {
    register: field,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { email: "", password: "" },
  });
  const title = verify
    ? "Make yourself at home."
    : reset
      ? "A fresh start."
      : register
        ? "Your next chapter starts here."
        : forgot
          ? "Let’s get you back in."
          : "Welcome to your quiet corner.";
  async function submit(data: { email: string; password: string }) {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      if (register || reset) {
        const bytes = new TextEncoder().encode(data.password).length;
        if (bytes < 12 || bytes > 72)
          throw new Error("Use a password of 12–72 UTF-8 bytes.");
      }
      if (verify) {
        if (!token)
          throw new Error("This verification link is missing its token.");
        await send("/auth/verify-email", "POST", { token });
        setMessage("Email verified. You can now sign in.");
        history.replaceState(null, "", location.pathname);
      } else if (reset) {
        await send("/auth/reset-password", "POST", {
          token,
          password: data.password,
        });
        setMessage("Password updated. Please sign in again.");
        history.replaceState(null, "", location.pathname);
      } else if (forgot) {
        await send("/auth/forgot-password", "POST", { email: data.email });
        setMessage("If a verified account exists, a reset link is on its way.");
      } else if (register) {
        await send("/auth/register", "POST", data);
        setMessage(
          "If this email can be registered, a verification link is on its way. Check your inbox.",
        );
      } else {
        await send("/auth/login", "POST", data);
        onLogin();
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="auth-scene">
      <div className="auth-story">
        <Link to="/" className="brand">
          <BookOpen size={25} />
          <span>
            life tracker<span className="brand-dot">.</span>
          </span>
        </Link>
        <div>
          <p className="eyebrow">A LITTLE SPACE TO GROW</p>
          <h1>
            Every book
            <br />
            leaves a little
            <br />
            <em>something.</em>
          </h1>
          <p>
            Keep the stories. Remember the lessons.
            <br />
            Build a library that feels like you.
          </p>
        </div>
        <p className="auth-foot">ONE BOOK, ONE CHAPTER AT A TIME.</p>
      </div>
      <section className="auth-card">
        <BookOpen className="auth-mark" />
        <p className="eyebrow">KNOWLEDGE LIBRARY</p>
        <h2>{title}</h2>
        <p className="muted">
          {verify
            ? "Confirm your email to open your private library."
            : register
              ? "A personal home for the books you’ve finished."
              : "Your books and reflections are waiting for you."}
        </p>
        <form
          onSubmit={
            verify || reset
              ? (e) => {
                  e.preventDefault();
                  submit(getValues());
                }
              : handleSubmit(submit)
          }
        >
          {!verify && !reset && (
            <label>
              Email address
              <input autoComplete="email" type="email" {...field("email")} />
              {errors.email && (
                <span className="field-error">{errors.email.message}</span>
              )}
            </label>
          )}
          {!verify && !forgot && (
            <label>
              Password
              <input
                type="password"
                autoComplete={
                  register || reset ? "new-password" : "current-password"
                }
                required
                minLength={register || reset ? 12 : 1}
                maxLength={72}
                {...field("password")}
              />
              {(register || reset) && (
                <small>12–72 UTF-8 bytes. A long passphrase works well.</small>
              )}
            </label>
          )}
          {error && (
            <p className="error" role="alert">
              {error}
            </p>
          )}
          {message && (
            <p className="success" role="status">
              {message}
            </p>
          )}
          {!message && (
            <button className="primary full" disabled={busy}>
              {busy
                ? "Just a moment…"
                : verify
                  ? "Verify email"
                  : reset
                    ? "Save new password"
                    : register
                      ? "Create your library"
                      : forgot
                        ? "Send reset link"
                        : "Step inside"}
              <ArrowRight size={17} />
            </button>
          )}
        </form>
        {page === "login" && (
          <>
            <Link className="text-link" to="/forgot-password">
              Forgot your password?
            </Link>
            <p className="auth-switch">
              New here? <Link to="/register">Create an account</Link>
            </p>
            <button
              className="text-link"
              disabled={busy}
              onClick={async () => {
                setError("");
                try {
                  setBusy(true);
                  await send("/auth/resend-verification", "POST", {
                    email: getValues("email"),
                  });
                  setMessage(
                    "If an unverified account exists, a verification link is on its way.",
                  );
                } catch (e) {
                  setError(errorMessage(e));
                } finally {
                  setBusy(false);
                }
              }}
            >
              Resend verification email
            </button>
          </>
        )}
        {page !== "login" && (
          <Link className="text-link" to="/login">
            Back to sign in
          </Link>
        )}
        <p className="privacy-note">
          Your library is private. Your story is yours.
        </p>
      </section>
    </main>
  );
}
