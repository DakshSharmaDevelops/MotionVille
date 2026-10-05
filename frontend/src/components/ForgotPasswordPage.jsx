import { useEffect, useState } from "react";
import { Icon, PasswordInput } from "./ui.jsx";
import { API_BASE_URL } from "../api/videoApi.js";

export default function ForgotPasswordPage({ token: initialToken, onNavigateLogin, onNavigateForgot }) {
  const [token, setToken] = useState(initialToken || "");
  const [emailOrUsername, setEmailOrUsername] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [resetSuccess, setResetSuccess] = useState(false);

  // Token validation state
  const [validatingToken, setValidatingToken] = useState(Boolean(initialToken));
  const [tokenValid, setTokenValid] = useState(false);
  const [tokenError, setTokenError] = useState("");

  useEffect(() => {
    if (!token) {
      setValidatingToken(false);
      return;
    }

    let isMounted = true;
    setValidatingToken(true);
    setTokenError("");

    fetch(`${API_BASE_URL}/auth/reset-password/validate?token=${encodeURIComponent(token)}`, {
      credentials: "include",
    })
      .then(async (res) => {
        const data = await res.json().catch(() => ({}));
        if (!isMounted) return;
        if (res.ok && data.success) {
          setTokenValid(true);
        } else {
          setTokenValid(false);
          setTokenError(data.message || "This password reset link is invalid or has expired.");
        }
      })
      .catch((err) => {
        if (!isMounted) return;
        setTokenValid(false);
        setTokenError(err.message || "Unable to validate reset token.");
      })
      .finally(() => {
        if (isMounted) setValidatingToken(false);
      });

    return () => {
      isMounted = false;
    };
  }, [token]);

  async function handleSendResetLink(event) {
    event.preventDefault();
    if (!emailOrUsername.trim()) {
      setError("Please enter your email or username.");
      return;
    }

    setBusy(true);
    setError("");

    try {
      const res = await fetch(`${API_BASE_URL}/auth/forgot-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ emailOrUsername: emailOrUsername.trim() }),
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        throw new Error(data.message || "Failed to process password reset request.");
      }

      setSubmitted(true);
    } catch (err) {
      setError(err.message || "Something went wrong. Please try again.");
    } finally {
      setBusy(false);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();
    setError("");

    if (!newPassword) {
      setError("Please enter a new password.");
      return;
    }

    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match. Please ensure both fields are identical.");
      return;
    }

    setBusy(true);

    try {
      const res = await fetch(`${API_BASE_URL}/auth/reset-password`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          token,
          newPassword,
        }),
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        throw new Error(data.message || "Failed to reset password.");
      }

      setResetSuccess(true);
    } catch (err) {
      setError(err.message || "Failed to reset password. Please request a new link.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="auth-page-container">
      <div className="auth-card">
        {/* State A: Validating token */}
        {validatingToken && (
          <div style={{ textAlign: "center", padding: "20px 0" }}>
            <div style={{ fontSize: 14, color: "#64748b", marginBottom: 12 }}>
              Verifying your reset link…
            </div>
          </div>
        )}

        {/* State B: Token is invalid / expired */}
        {!validatingToken && token && !tokenValid && !resetSuccess && (
          <div style={{ textAlign: "center" }}>
            <div
              style={{
                width: 48,
                height: 48,
                borderRadius: "50%",
                background: "#fef2f2",
                color: "#ef4444",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: 16,
              }}
            >
              <Icon name="close" size={24} />
            </div>

            <h2 style={{ fontSize: 20, fontWeight: 700, color: "#0f172a", marginBottom: 8 }}>
              Reset Link Expired or Invalid
            </h2>

            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, marginBottom: 24 }}>
              {tokenError || "This password reset link is invalid, expired, or has already been used."}
            </p>

            <div style={{ display: "grid", gap: 10 }}>
              <button
                type="button"
                className="button button-primary"
                onClick={() => {
                  setToken("");
                  setTokenError("");
                  if (onNavigateForgot) onNavigateForgot();
                }}
              >
                Request a new link
              </button>

              <button
                type="button"
                className="text-button"
                onClick={onNavigateLogin}
                style={{ textAlign: "center" }}
              >
                Back to Sign in
              </button>
            </div>
          </div>
        )}

        {/* State C: Reset password form (Token is valid) */}
        {!validatingToken && token && tokenValid && !resetSuccess && (
          <div>
            <div style={{ textAlign: "center", marginBottom: 24 }}>
              <h2 style={{ fontSize: 22, fontWeight: 800, color: "#0f172a", marginBottom: 6 }}>
                Create New Password
              </h2>
              <p style={{ fontSize: 13, color: "#64748b", margin: 0 }}>
                Please enter and confirm your new password below.
              </p>
            </div>

            <form onSubmit={handleResetPassword} className="dialog-form">
              <label>
                New Password
                <PasswordInput
                  name="newPassword"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="At least 6 characters"
                  autoComplete="new-password"
                  minLength={6}
                  required
                />
              </label>

              <label>
                Confirm Password
                <PasswordInput
                  name="confirmPassword"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="Re-enter your new password"
                  autoComplete="new-password"
                  minLength={6}
                  required
                />
              </label>

              {error && (
                <p className="inline-error" role="alert">
                  {error}
                </p>
              )}

              <div style={{ marginTop: 8, display: "grid", gap: 10 }}>
                <button
                  type="submit"
                  className="button button-primary"
                  disabled={busy}
                >
                  {busy ? "Updating password…" : "Reset Password"}
                  {!busy && <Icon name="chevron" size={17} />}
                </button>

                <button
                  type="button"
                  className="text-button"
                  onClick={onNavigateLogin}
                  disabled={busy}
                  style={{ textAlign: "center" }}
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* State D: Password successfully reset */}
        {resetSuccess && (
          <div style={{ textAlign: "center" }}>
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: "50%",
                background: "#ecfdf5",
                color: "#10b981",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: 16,
              }}
            >
              <Icon name="check" size={26} />
            </div>

            <h2 style={{ fontSize: 20, fontWeight: 700, color: "#0f172a", marginBottom: 8 }}>
              Password Reset Complete!
            </h2>

            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, marginBottom: 24 }}>
              Your password has been securely updated. You can now sign in with your new credentials.
            </p>

            <button
              type="button"
              className="button button-primary"
              onClick={onNavigateLogin}
              style={{ width: "100%", justifyContent: "center" }}
            >
              Sign in now
              <Icon name="chevron" size={17} />
            </button>
          </div>
        )}

        {/* State E: Request password reset link (No token) */}
        {!token && !submitted && (
          <div>
            <div style={{ textAlign: "center", marginBottom: 22 }}>
              <div
                style={{
                  width: 48,
                  height: 48,
                  borderRadius: "50%",
                  background: "#f1f5f9",
                  color: "#0f172a",
                  display: "inline-flex",
                  alignItems: "center",
                  justifyContent: "center",
                  marginBottom: 14,
                }}
              >
                <Icon name="sparkle" size={22} />
              </div>

              <h2 style={{ fontSize: 22, fontWeight: 800, color: "#0f172a", marginBottom: 6 }}>
                Forgot your password?
              </h2>

              <p style={{ fontSize: 13, color: "#64748b", margin: 0, lineHeight: 1.5 }}>
                Enter your email or username and we'll send you a link to reset your password.
              </p>
            </div>

            <form onSubmit={handleSendResetLink} className="dialog-form">
              <label>
                Username or Email
                <input
                  name="emailOrUsername"
                  type="text"
                  value={emailOrUsername}
                  onChange={(e) => setEmailOrUsername(e.target.value)}
                  placeholder="e.g. suresh or suresh@example.com"
                  autoComplete="username"
                  required
                />
              </label>

              {error && (
                <p className="inline-error" role="alert">
                  {error}
                </p>
              )}

              <div style={{ marginTop: 8, display: "grid", gap: 10 }}>
                <button
                  type="submit"
                  className="button button-primary"
                  disabled={busy}
                >
                  {busy ? "Sending reset link…" : "Send Reset Link"}
                  {!busy && <Icon name="chevron" size={17} />}
                </button>

                <button
                  type="button"
                  className="text-button"
                  onClick={onNavigateLogin}
                  disabled={busy}
                  style={{ textAlign: "center" }}
                >
                  Back to Sign in
                </button>
              </div>
            </form>
          </div>
        )}

        {/* State F: Link sent confirmation */}
        {!token && submitted && (
          <div style={{ textAlign: "center" }}>
            <div
              style={{
                width: 52,
                height: 52,
                borderRadius: "50%",
                background: "#f0fdf4",
                color: "#16a34a",
                display: "inline-flex",
                alignItems: "center",
                justifyContent: "center",
                marginBottom: 16,
              }}
            >
              <Icon name="check" size={26} />
            </div>

            <h2 style={{ fontSize: 20, fontWeight: 700, color: "#0f172a", marginBottom: 8 }}>
              Check your email
            </h2>

            <p style={{ fontSize: 13, color: "#64748b", lineHeight: 1.5, marginBottom: 14 }}>
              If an account exists for <strong>{emailOrUsername}</strong>, we have sent instructions to reset your password.
            </p>

            <div
              style={{
                background: "#f8fafc",
                border: "1px solid #e2e8f0",
                borderRadius: 8,
                padding: "10px 14px",
                fontSize: 12,
                color: "#64748b",
                textAlign: "left",
                marginBottom: 20,
              }}
            >
              💡 <em>Check your spam or junk folder if you don't see the email within a couple minutes. (In local development, check backend console logs for the reset link).</em>
            </div>

            <div style={{ display: "grid", gap: 10 }}>
              <button
                type="button"
                className="button"
                onClick={() => {
                  setSubmitted(false);
                  setEmailOrUsername("");
                }}
              >
                Send another link
              </button>

              <button
                type="button"
                className="text-button"
                onClick={onNavigateLogin}
                style={{ textAlign: "center" }}
              >
                Back to Sign in
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
