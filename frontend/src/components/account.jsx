import { useEffect, useState } from "react";
import { Avatar, Icon, Modal, PasswordInput } from "./ui.jsx";
import { API_BASE_URL, apiRequest } from "../api/videoApi.js";

export function AccountDialog({
  mode,
  user,
  myChannels,
  subscriptionChannels,
  onClose,
  onLogin,
  onRegister,
  onUpdate,
  onLogout,
  onCreateChannel,
  onShowSubscriptions,
  onSwitchLogin,
  onSwitchRegister,
  onResendVerification,
  onForgotPassword,
  onDeleteAccount,
}) {
  const authMode = mode === "auth";
  const loginMode = mode === "login";
  const registerMode = mode === "register";
  const profileMode = mode === "profile";

  const [form, setForm] = useState({
    username: user?.username || "",
    email: user?.email || "",
    password: "",
    displayName: user?.displayName || "",
    avatarUrl: user?.avatarUrl || "",
  });

  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  // Verification step state
  const [verificationStep, setVerificationStep] = useState(false);
  const [pendingEmail, setPendingEmail] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [otpBusy, setOtpBusy] = useState(false);
  const [otpError, setOtpError] = useState("");
  const [resendCooldown, setResendCooldown] = useState(0);
  const [resendSuccess, setResendSuccess] = useState(false);
  const [verifiedSuccess, setVerifiedSuccess] = useState(false);

  // Reset password form state (inside Profile/Manage Account)
  const [resetPasswordForm, setResetPasswordForm] = useState({
    newPassword: "",
    confirmPassword: "",
  });
  const [resetPasswordBusy, setResetPasswordBusy] = useState(false);
  const [resetPasswordError, setResetPasswordError] = useState("");
  const [resetPasswordSuccess, setResetPasswordSuccess] = useState(false);
  const [sendingResetLink, setSendingResetLink] = useState(false);
  const [resetLinkSent, setResetLinkSent] = useState(false);

  // Delete account state
  const [deleteConfirmText, setDeleteConfirmText] = useState("");
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  async function handleResetPasswordInProfile(event) {
    event.preventDefault();
    setResetPasswordError("");
    setResetPasswordSuccess(false);

    if (resetPasswordForm.newPassword.length < 6) {
      setResetPasswordError("Password must be at least 6 characters.");
      return;
    }
    if (resetPasswordForm.newPassword !== resetPasswordForm.confirmPassword) {
      setResetPasswordError("Passwords do not match.");
      return;
    }

    setResetPasswordBusy(true);
    try {
      await apiRequest(`/users/${user.id}/reset-password`, {
        method: "POST",
        body: JSON.stringify({
          newPassword: resetPasswordForm.newPassword,
        }),
      });
      setResetPasswordSuccess(true);
      setResetPasswordForm({ newPassword: "", confirmPassword: "" });
      setTimeout(() => setResetPasswordSuccess(false), 5000);
    } catch (err) {
      setResetPasswordError(err.message || "Failed to reset password.");
    } finally {
      setResetPasswordBusy(false);
    }
  }

  async function handleSendResetLinkToEmail() {
    if (sendingResetLink || !user?.email) return;
    setSendingResetLink(true);
    setResetPasswordError("");
    try {
      await apiRequest("/auth/forgot-password", {
        method: "POST",
        body: JSON.stringify({
          emailOrUsername: user.email,
        }),
      });
      setResetLinkSent(true);
      setTimeout(() => setResetLinkSent(false), 6000);
    } catch (err) {
      setResetPasswordError(err.message || "Failed to send reset link.");
    } finally {
      setSendingResetLink(false);
    }
  }

  async function handleDeleteAccount(event) {
    event.preventDefault();
    if (deleteConfirmText !== "DELETE" && deleteConfirmText !== user?.username) {
      setDeleteError(`Please type "DELETE" or "${user?.username}" to confirm.`);
      return;
    }

    if (!window.confirm("Are you sure you want to permanently delete your account? All your channels, videos, playlists, and comments will be permanently erased.")) {
      return;
    }

    setDeleteBusy(true);
    setDeleteError("");
    try {
      if (onDeleteAccount) {
        await onDeleteAccount();
      } else {
        await apiRequest(`/users/${user.id}`, { method: "DELETE" });
        window.location.href = "/";
      }
    } catch (err) {
      setDeleteError(err.message || "Failed to delete account. Please try again.");
      setDeleteBusy(false);
    }
  }

  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setInterval(() => {
      setResendCooldown((t) => Math.max(0, t - 1));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendCooldown]);

  // Background polling to detect if user verified via the email link
  useEffect(() => {
    if (!verificationStep || verifiedSuccess || !pendingEmail) return;

    let isMounted = true;
    const poll = setInterval(async () => {
      try {
        const res = await fetch(
          `${API_BASE_URL}/auth/verification-status?email=${encodeURIComponent(pendingEmail)}`
        );
        if (!res.ok) return;
        const data = await res.json();
        if (data.verified && isMounted) {
          setVerifiedSuccess(true);
          clearInterval(poll);
          setTimeout(() => {
            onLogin({ username: form.username, password: form.password });
          }, 900);
        }
      } catch {
        // Continue polling silently
      }
    }, 2500);

    return () => {
      isMounted = false;
      clearInterval(poll);
    };
  }, [verificationStep, verifiedSuccess, pendingEmail, form.username, form.password, onLogin]);

  async function handleVerifyOtp(event) {
    event.preventDefault();
    if (!otpCode.trim()) {
      setOtpError("Please enter the 6-digit code.");
      return;
    }
    setOtpBusy(true);
    setOtpError("");

    try {
      const res = await fetch(`${API_BASE_URL}/auth/verify-otp`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          email: pendingEmail,
          otp: otpCode.trim(),
        }),
      });

      const data = await res.json().catch(() => ({}));
      if (!res.ok) {
        throw new Error(data.message || "Invalid or expired verification code.");
      }

      setVerifiedSuccess(true);
      setTimeout(() => {
        onLogin({ username: form.username, password: form.password });
      }, 900);
    } catch (err) {
      setOtpError(err.message || "Failed to verify code.");
    } finally {
      setOtpBusy(false);
    }
  }

  async function handleResendCode() {
    if (resendCooldown > 0 || otpBusy) return;
    setOtpBusy(true);
    setOtpError("");
    try {
      await onResendVerification(pendingEmail);
      setResendSuccess(true);
      setResendCooldown(60);
      setTimeout(() => setResendSuccess(false), 5000);
    } catch (err) {
      setOtpError(err.message || "Failed to resend verification email.");
    } finally {
      setOtpBusy(false);
    }
  }

  function update(event) {
    setForm((current) => ({
      ...current,
      [event.target.name]: event.target.value,
    }));
  }

  function openLogin() {
    setError("");
    setForm({
      username: "",
      email: "",
      password: "",
      displayName: "",
      avatarUrl: "",
    });
    onSwitchLogin();
  }

  function openRegister() {
    setError("");
    setForm({
      username: "",
      email: "",
      password: "",
      displayName: "",
      avatarUrl: "",
    });
    onSwitchRegister();
  }

  async function submit(event) {
    event.preventDefault();

    if (loginMode) {
      if (!form.username.trim() || !form.password) {
        setError("Username/email and password are required.");
        return;
      }
    } else if (registerMode) {
      if (
        !form.username.trim() ||
        !form.email.trim() ||
        !form.displayName.trim() ||
        !form.password
      ) {
        setError("Username, email, display name and password are required.");
        return;
      }
    } else if (profileMode) {
      if (
        !form.username.trim() ||
        !form.email.trim() ||
        !form.displayName.trim()
      ) {
        setError("Username, email and display name are required.");
        return;
      }
    }

    setBusy(true);
    setError("");

    try {
      if (loginMode) {
        await onLogin(form);
      } else if (registerMode) {
        await onRegister(form);
        setPendingEmail(form.email.trim());
        setVerificationStep(true);
        setResendCooldown(60);
        setError("");
      } else if (profileMode) {
        await onUpdate(form);
      }
    } catch (requestError) {
      setError(
        requestError.message ||
          "Something went wrong. Please try again."
      );
    } finally {
      setBusy(false);
    }
  }

  if (authMode) {
    return (
      <Modal onClose={onClose}>
        <section
          className="dialog create-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="auth-dialog-title"
        >
          <div className="dialog-topline">
            <span className="dialog-step">
              <Icon name="sparkle" size={15} />
              MotionVille account
            </span>

            <button
              className="icon-button"
              onClick={onClose}
              aria-label="Close"
            >
              <Icon name="close" />
            </button>
          </div>

          <div style={{ textAlign: "center", marginBottom: 22 }}>
            <Avatar name="M" size="large" />

            <h2 id="auth-dialog-title" style={{ marginTop: 14, marginBottom: 5 }}>
              Welcome to MotionVille
            </h2>

            <p className="dialog-subtitle" style={{ marginBottom: 0 }}>
              Sign in to your account or create a new one.
            </p>
          </div>

          <div style={{ display: "grid", gap: 10 }}>
            <button
              type="button"
              className="button button-primary"
              onClick={openLogin}
            >
              Sign in
              <Icon name="chevron" size={17} />
            </button>

            <button
              type="button"
              className="button"
              onClick={openRegister}
            >
              Create account
            </button>
          </div>
        </section>
      </Modal>
    );
  }

  if (registerMode && verificationStep) {
    return (
      <Modal onClose={onClose}>
        <section
          className="dialog create-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="account-dialog-title"
        >
          <div className="dialog-topline">
            <span className="dialog-step">
              <Icon name="sparkle" size={15} />
              Verify your email
            </span>

            <button
              className="icon-button"
              onClick={onClose}
              aria-label="Close"
            >
              <Icon name="close" />
            </button>
          </div>

          {verifiedSuccess ? (
            <div style={{ textAlign: "center", padding: "20px 0 10px" }}>
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
                  margin: "0 auto 16px",
                }}
              >
                <Icon name="check" size={28} />
              </div>

              <h2 style={{ fontSize: 20, fontWeight: 700, color: "#0f172a", marginBottom: 6 }}>
                Email Verified!
              </h2>

              <p style={{ fontSize: 13, color: "#64748b", margin: 0 }}>
                Logging you in to your new MotionVille account…
              </p>
            </div>
          ) : (
            <div>
              <div style={{ textAlign: "center", marginBottom: 20 }}>
                <h2 id="account-dialog-title" style={{ fontSize: 20, fontWeight: 700, color: "#0f172a", marginBottom: 6 }}>
                  Verify your account
                </h2>

                <p className="dialog-subtitle" style={{ fontSize: 13, color: "#64748b", margin: 0, lineHeight: 1.5 }}>
                  We sent a 6-digit code and a verification link to<br />
                  <strong style={{ color: "#0f172a" }}>{pendingEmail}</strong>
                </p>
              </div>

              <div style={{ display: "grid", gap: 16 }}>
                {/* Option 1: Enter 6-digit code */}
                <form onSubmit={handleVerifyOtp} className="dialog-form">
                  <label style={{ fontSize: 11, fontWeight: 700, color: "#334155" }}>
                    Option 1: Enter 6-digit code
                    <input
                      name="otp"
                      type="text"
                      inputMode="numeric"
                      pattern="[0-9]*"
                      maxLength={6}
                      value={otpCode}
                      onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, ""))}
                      placeholder="123456"
                      style={{
                        textAlign: "center",
                        fontSize: 22,
                        letterSpacing: 8,
                        fontWeight: 700,
                        padding: "8px 12px",
                      }}
                      autoFocus
                    />
                  </label>

                  {otpError && (
                    <p className="inline-error" role="alert" style={{ margin: 0 }}>
                      {otpError}
                    </p>
                  )}

                  <button
                    type="submit"
                    className="button button-primary"
                    disabled={otpBusy || otpCode.length !== 6}
                    style={{ justifyContent: "center" }}
                  >
                    {otpBusy ? "Verifying…" : "Verify Code"}
                    {!otpBusy && <Icon name="chevron" size={17} />}
                  </button>
                </form>

                {/* Divider */}
                <div style={{ display: "flex", alignItems: "center", gap: 12, margin: "2px 0" }}>
                  <div style={{ flex: 1, height: 1, background: "#e2e8f0" }} />
                  <span style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8" }}>OR</span>
                  <div style={{ flex: 1, height: 1, background: "#e2e8f0" }} />
                </div>

                {/* Option 2: Click the link */}
                <div
                  style={{
                    padding: "12px 14px",
                    borderRadius: 10,
                    background: "#f8fafc",
                    border: "1px solid #e2e8f0",
                    textAlign: "center",
                  }}
                >
                  <div style={{ fontSize: 12, fontWeight: 700, color: "#334155", marginBottom: 3 }}>
                    Option 2: Click the link in your email
                  </div>
                  <p style={{ margin: 0, fontSize: 12, color: "#64748b", lineHeight: 1.4 }}>
                    Click <strong>Verify Email Address</strong> in the email we sent you.
                  </p>
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 6, marginTop: 8, fontSize: 11, color: "#2563eb", fontWeight: 600 }}>
                    <span style={{ display: "inline-block", width: 8, height: 8, borderRadius: "50%", background: "#2563eb", opacity: 0.8 }} />
                    Listening for verification…
                  </div>
                </div>

                {resendSuccess && (
                  <p style={{ margin: 0, fontSize: 11, color: "#16a34a", textAlign: "center", fontWeight: 600 }}>
                    ✓ New verification code and link sent! Check your inbox.
                  </p>
                )}

                {/* Footer Controls */}
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", paddingTop: 4 }}>
                  <button
                    type="button"
                    className="text-button"
                    onClick={() => {
                      setVerificationStep(false);
                      setOtpError("");
                    }}
                    style={{ fontSize: 11, color: "#64748b" }}
                  >
                    Edit details
                  </button>

                  <button
                    type="button"
                    className="text-button"
                    disabled={resendCooldown > 0 || otpBusy}
                    onClick={handleResendCode}
                    style={{
                      fontSize: 11,
                      color: resendCooldown > 0 ? "#94a3b8" : "#2563eb",
                      fontWeight: 600,
                    }}
                  >
                    {resendCooldown > 0 ? `Resend in ${resendCooldown}s` : "Resend code & link"}
                  </button>
                </div>
              </div>
            </div>
          )}
        </section>
      </Modal>
    );
  }

  return (
    <Modal onClose={onClose}>
      <section
        className="dialog create-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="account-dialog-title"
      >
        <div className="dialog-topline">
          <span className="dialog-step">
            <Icon name="sparkle" size={15} />
            {loginMode
              ? "Welcome back"
              : registerMode
              ? "Join MotionVille"
              : "Your profile"}
          </span>

          <button
            className="icon-button"
            onClick={onClose}
            aria-label="Close"
          >
            <Icon name="close" />
          </button>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 18 }}>
          {!loginMode && (
            <Avatar
              src={form.avatarUrl}
              name={form.displayName || form.username || "M"}
              size="large"
            />
          )}

          <div>
            <h2 id="account-dialog-title" style={{ marginBottom: 3 }}>
              {loginMode
                ? "Sign in"
                : registerMode
                ? "Create your profile"
                : "Profile"}
            </h2>

            <p className="dialog-subtitle" style={{ marginBottom: 0 }}>
              {loginMode
                ? "Sign in to your MotionVille account."
                : registerMode
                ? "Create your MotionVille account."
                : "Update your profile details and manage your account."}
            </p>
          </div>
        </div>

        <form className="dialog-form" onSubmit={submit}>
          {loginMode ? (
            <>
              <label>
                Username or Email
                <input
                  name="username"
                  value={form.username}
                  onChange={update}
                  placeholder="Username or email"
                  autoComplete="username"
                  required
                />
              </label>

              <label>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span>Password</span>
                  {onForgotPassword && (
                    <button
                      type="button"
                      className="text-button"
                      onClick={onForgotPassword}
                      style={{ fontSize: 11, color: "#2563eb", padding: 0, textDecoration: "none", cursor: "pointer", fontWeight: 500 }}
                    >
                      Forgot password?
                    </button>
                  )}
                </div>
                <PasswordInput
                  name="password"
                  value={form.password}
                  onChange={update}
                  placeholder="Your password"
                  autoComplete="current-password"
                  required
                />
              </label>
            </>
          ) : (
            <>
              <div className="form-two-col">
                <label>
                  Username
                  <input
                    name="username"
                    value={form.username}
                    onChange={update}
                    maxLength="50"
                    placeholder="yourusername"
                    autoComplete="username"
                    required
                  />
                </label>

                <label>
                  Display name
                  <input
                    name="displayName"
                    value={form.displayName}
                    onChange={update}
                    maxLength="100"
                    placeholder="Your name"
                    required
                  />
                </label>
              </div>

              <label>
                Email
                <input
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={update}
                  placeholder="you@example.com"
                  autoComplete="email"
                  required
                />
              </label>

              {registerMode && (
                <label>
                  Password <span className="optional">Required</span>
                  <PasswordInput
                    name="password"
                    value={form.password}
                    onChange={update}
                    placeholder="Create a password"
                    autoComplete="new-password"
                    required={registerMode}
                  />
                </label>
              )}

              <label>
                Avatar URL <span className="optional">Optional</span>
                <input
                  name="avatarUrl"
                  type="url"
                  value={form.avatarUrl}
                  onChange={update}
                  placeholder="https://example.com/avatar.jpg"
                />
              </label>
            </>
          )}

          {error && (
            <div style={{ display: "grid", gap: 8 }}>
              <p className="inline-error" role="alert">
                {error}
              </p>
              {loginMode && (error.toLowerCase().includes("verif") || error.toLowerCase().includes("not verified")) && onResendVerification && (
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", background: "#fef3c7", padding: "8px 12px", borderRadius: 8, fontSize: 12, color: "#92400e" }}>
                  <span>Did not receive verification email?</span>
                  <button
                    type="button"
                    className="feed-filter"
                    style={{ padding: "4px 10px", fontSize: 11, cursor: "pointer", background: "#fff", borderColor: "#fcd34d" }}
                    onClick={() => onResendVerification(form.username)}
                  >
                    Resend email
                  </button>
                </div>
              )}
            </div>
          )}

          {profileMode && (
            <div style={{ display: "grid", gap: 8, padding: 12, border: "1px solid #efeeec", borderRadius: 10, background: "#fcfbfa" }}>
              <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <strong style={{ fontSize: 11 }}>Your account</strong>
                <span style={{ fontSize: 11, fontWeight: 600, color: user?.emailVerified ? "#16a34a" : "#d97706" }}>
                  {user?.emailVerified ? "✓ Verified" : "⚠ Unverified"}
                </span>
              </div>
              {!user?.emailVerified && onResendVerification && (
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", background: "#fef3c7", padding: "6px 10px", borderRadius: 6, fontSize: 11, color: "#92400e" }}>
                  <span>Verify your email to unlock all features.</span>
                  <button type="button" className="feed-filter" style={{ padding: "3px 8px", fontSize: 10, cursor: "pointer", background: "#fff", borderColor: "#fcd34d" }} onClick={onResendVerification}>
                    Resend link
                  </button>
                </div>
              )}
              <span style={{ color: "#85858d", fontSize: 10 }}>
                {myChannels.length} channel{myChannels.length === 1 ? "" : "s"} · {subscriptionChannels.length} subscription{subscriptionChannels.length === 1 ? "" : "s"}
              </span>

              <div style={{ display: "flex", gap: 7, flexWrap: "wrap" }}>
                {myChannels.length === 0 && (
                  <button type="button" className="feed-filter" onClick={onCreateChannel}>
                    Create channel
                  </button>
                )}
                <button type="button" className="feed-filter" onClick={onShowSubscriptions}>
                  View subscriptions
                </button>
              </div>
            </div>
          )}

          <div className="dialog-actions">
            {loginMode && (
              <button
                type="button"
                className="text-button"
                onClick={() => {
                  setForm({ username: "", email: "", password: "", displayName: "", avatarUrl: "" });
                  setError("");
                }}
              >
                Clear
              </button>
            )}

            {profileMode && (
              <button type="button" className="text-button" onClick={onLogout} disabled={busy}>
                Sign out
              </button>
            )}

            <button type="button" className="text-button" onClick={onClose} disabled={busy}>
              Cancel
            </button>

            <button className="button button-primary" type="submit" disabled={busy}>
              {busy
                ? "Please wait…"
                : loginMode
                ? "Sign in"
                : registerMode
                ? "Create profile"
                : "Save profile"}
              {!busy && <Icon name="chevron" size={17} />}
            </button>
          </div>

          {(loginMode || registerMode) && (
            <div style={{ textAlign: "center", marginTop: 4 }}>
              <span style={{ color: "#85858d", fontSize: 10 }}>
                {loginMode ? "New to MotionVille?" : "Already have an account?"}
              </span>{" "}
              <button
                type="button"
                className="text-button"
                onClick={() => {
                  loginMode ? onSwitchRegister() : onSwitchLogin();
                }}
                disabled={busy}
                style={{ padding: 0, fontSize: 10 }}
              >
                {loginMode ? "Create account" : "Sign in"}
              </button>
            </div>
          )}
        </form>

        {profileMode && (
          <>
            <div className="account-section-card">
              <div className="account-section-header">
                <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <Icon name="sparkle" size={16} />
                  <strong style={{ fontSize: 13, color: "#1e293b" }}>Reset Password</strong>
                </div>
                <span style={{ fontSize: 11, color: "#64748b" }}>Choose a new password</span>
              </div>

              <form onSubmit={handleResetPasswordInProfile} className="dialog-form">
                <label>
                  New Password
                  <PasswordInput
                    name="newPassword"
                    value={resetPasswordForm.newPassword}
                    onChange={(e) =>
                      setResetPasswordForm((p) => ({ ...p, newPassword: e.target.value }))
                    }
                    placeholder="At least 6 characters"
                    autoComplete="new-password"
                    minLength={6}
                    required
                  />
                </label>

                <label>
                  Confirm New Password
                  <PasswordInput
                    name="confirmPassword"
                    value={resetPasswordForm.confirmPassword}
                    onChange={(e) =>
                      setResetPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))
                    }
                    placeholder="Re-enter new password"
                    autoComplete="new-password"
                    minLength={6}
                    required
                  />
                </label>

                {resetPasswordError && (
                  <p className="inline-error" role="alert" style={{ margin: 0 }}>
                    {resetPasswordError}
                  </p>
                )}

                {resetPasswordSuccess && (
                  <p
                    style={{
                      margin: 0,
                      padding: "8px 12px",
                      borderRadius: 8,
                      background: "#ecfdf5",
                      color: "#059669",
                      fontSize: 12,
                      fontWeight: 600,
                    }}
                  >
                    ✓ Password successfully updated!
                  </p>
                )}

                {resetLinkSent && (
                  <p
                    style={{
                      margin: 0,
                      padding: "8px 12px",
                      borderRadius: 8,
                      background: "#eff6ff",
                      color: "#2563eb",
                      fontSize: 12,
                      fontWeight: 600,
                    }}
                  >
                    ✓ Password reset link sent to {user?.email}! Check your inbox.
                  </p>
                )}

                <div
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: 10,
                    marginTop: 4,
                  }}
                >
                  <button
                    type="button"
                    className="text-button"
                    onClick={handleSendResetLinkToEmail}
                    disabled={sendingResetLink}
                    style={{ fontSize: 11, color: "#2563eb", padding: 0, fontWeight: 500 }}
                  >
                    {sendingResetLink ? "Sending link…" : "Or send reset link to my email"}
                  </button>

                  <button
                    type="submit"
                    className="button"
                    disabled={
                      resetPasswordBusy ||
                      !resetPasswordForm.newPassword ||
                      !resetPasswordForm.confirmPassword
                    }
                    style={{ padding: "6px 14px", fontSize: 12, fontWeight: 600 }}
                  >
                    {resetPasswordBusy ? "Updating…" : "Reset Password"}
                  </button>
                </div>
              </form>
            </div>

            <div className="account-danger-zone">
              <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6 }}>
                <strong style={{ color: "#dc2626", fontSize: 13 }}>Delete Account</strong>
              </div>

              <p style={{ margin: "0 0 12px", color: "#64748b", fontSize: 12, lineHeight: 1.5 }}>
                Permanently delete your MotionVille account, your channels, videos, playlists, and comments. This action cannot be undone.
              </p>

              <form onSubmit={handleDeleteAccount} style={{ display: "grid", gap: 10 }}>
                <label style={{ fontSize: 11, fontWeight: 600, color: "#475569" }}>
                  To confirm, type <strong style={{ color: "#dc2626" }}>DELETE</strong> or your username:
                  <input
                    type="text"
                    value={deleteConfirmText}
                    onChange={(e) => setDeleteConfirmText(e.target.value)}
                    placeholder={`Type "DELETE" or "${user?.username}"`}
                    style={{ marginTop: 5 }}
                    required
                  />
                </label>

                {deleteError && (
                  <p className="inline-error" role="alert" style={{ margin: 0 }}>
                    {deleteError}
                  </p>
                )}

                <button
                  type="submit"
                  className="button button-danger"
                  disabled={
                    deleteBusy ||
                    (deleteConfirmText !== "DELETE" && deleteConfirmText !== user?.username)
                  }
                  style={{
                    background: "#dc2626",
                    color: "#fff",
                    borderColor: "#dc2626",
                    justifyContent: "center",
                    opacity:
                      deleteConfirmText === "DELETE" || deleteConfirmText === user?.username
                        ? 1
                        : 0.6,
                    cursor:
                      deleteConfirmText === "DELETE" || deleteConfirmText === user?.username
                        ? "pointer"
                        : "not-allowed",
                  }}
                >
                  {deleteBusy ? "Deleting account…" : "Permanently Delete Account"}
                </button>
              </form>
            </div>
          </>
        )}
      </section>
    </Modal>
  );
}

export function SubscribersDialog({ channel, subscribers, onClose }) {
  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="subscribers-dialog-title">
        <div className="dialog-topline">
          <span className="dialog-step"><Icon name="subscriptions" size={15} /> Channel audience</span>
          <button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </div>
        <h2 id="subscribers-dialog-title">Subscribers</h2>
        <p className="dialog-subtitle">People subscribed to {channel?.name || "this channel"}.</p>
        {subscribers.length ? (
          <div style={{ display: "grid", gap: 8 }}>
            {subscribers.map((subscriber) => (
              <div key={subscriber.id} style={{ display: "flex", alignItems: "center", gap: 10, padding: "10px 0", borderBottom: "1px solid #efeeec" }}>
                <Avatar src={subscriber.avatarUrl} name={subscriber.displayName || subscriber.username} size="small" />
                <div style={{ minWidth: 0 }}>
                  <strong style={{ display: "block", fontSize: 11 }}>{subscriber.displayName || subscriber.username}</strong>
                  <span style={{ color: "#898890", fontSize: 9 }}>@{subscriber.username}</span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="empty-feed" style={{ minHeight: 180 }}>
            <span><Icon name="subscriptions" size={25} /></span>
            <h2>No subscribers yet</h2>
            <p>Subscribers will appear here when people follow this channel.</p>
          </div>
        )}
      </section>
    </Modal>
  );
}

export function AccountMenu({
  user,
  myChannels,
  subscriptionChannels,
  onProfile,
  onHome,
  onHistory,
  onLiked,
  onPlaylists,
  onSubscriptions,
  onLogout,
}) {
  return (
    <div
      role="menu"
      aria-label="Account menu"
      style={{
        position: "absolute",
        top: "calc(100% + 10px)",
        right: 0,
        width: 300,
        maxWidth: "calc(100vw - 24px)",
        background: "#ffffff",
        border: "1px solid #e8e5e1",
        borderRadius: 14,
        boxShadow: "0 16px 45px rgba(30, 24, 20, 0.16)",
        padding: 8,
        zIndex: 1000,
      }}
    >
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          padding: "10px 10px 12px",
          borderBottom: "1px solid #efeeec",
          marginBottom: 6,
        }}
      >
        <Avatar
          src={user?.avatarUrl}
          name={user?.displayName || user?.username || "M"}
          size="large"
        />

        <div style={{ minWidth: 0, flex: 1 }}>
          <strong
            style={{
              display: "block",
              fontSize: 13,
              color: "#242229",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            {user?.displayName || user?.username || "MotionVille user"}
          </strong>

          <span
            style={{
              display: "block",
              marginTop: 3,
              fontSize: 10,
              color: "#85818a",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
          >
            @{user?.username || "user"}
          </span>

          <button
            type="button"
            onClick={onProfile}
            style={{
              border: 0,
              background: "transparent",
              padding: 0,
              marginTop: 7,
              color: "#e6534d",
              fontSize: 10,
              fontWeight: 600,
              cursor: "pointer",
            }}
          >
            Manage your account
          </button>
        </div>
      </div>

      <div
        style={{
          padding: "4px 0",
          borderBottom: "1px solid #efeeec",
          marginBottom: 4,
        }}
      >
        <AccountMenuItem icon="home" label="Home" onClick={onHome} />
        <AccountMenuItem icon="history" label="History" onClick={onHistory} />
        <AccountMenuItem icon="like" label="Liked videos" onClick={onLiked} />
        <AccountMenuItem icon="library" label="Playlists" onClick={onPlaylists} />
        <AccountMenuItem icon="subscriptions" label="Subscriptions" onClick={onSubscriptions} />
      </div>

      <div
        style={{
          padding: "7px 10px 5px",
          color: "#8b8790",
          fontSize: 9,
        }}
      >
        {myChannels.length} channel{myChannels.length === 1 ? "" : "s"} · {subscriptionChannels.length} subscription{subscriptionChannels.length === 1 ? "" : "s"}
      </div>

      <button
        type="button"
        onClick={onLogout}
        style={{
          width: "100%",
          display: "flex",
          alignItems: "center",
          gap: 12,
          border: 0,
          background: "transparent",
          borderRadius: 9,
          padding: "10px 12px",
          color: "#343039",
          fontSize: 11,
          cursor: "pointer",
          textAlign: "left",
        }}
      >
        <Icon name="close" size={17} />
        Sign out
      </button>
    </div>
  );
}

function AccountMenuItem({ icon, label, onClick }) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      style={{
        width: "100%",
        display: "flex",
        alignItems: "center",
        gap: 12,
        border: 0,
        background: "transparent",
        borderRadius: 9,
        padding: "10px 12px",
        color: "#343039",
        fontSize: 11,
        cursor: "pointer",
        textAlign: "left",
      }}
    >
      <Icon name={icon} size={17} />
      <span>{label}</span>
    </button>
  );
}