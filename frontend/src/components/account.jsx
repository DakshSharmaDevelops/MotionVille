import { useState } from "react";
import { Avatar, Icon, Modal } from "./ui.jsx";

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
                Password
                <input
                  name="password"
                  type="password"
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

              <label>
                Password <span className="optional">{registerMode ? "Required" : "Leave blank to keep current"}</span>
                <input
                  name="password"
                  type="password"
                  value={form.password}
                  onChange={update}
                  placeholder={registerMode ? "Create a password" : "New password"}
                  autoComplete={registerMode ? "new-password" : "new-password"}
                  required={registerMode}
                />
              </label>

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
            <p className="inline-error" role="alert">
              {error}
            </p>
          )}

          {profileMode && (
            <div style={{ display: "grid", gap: 8, padding: 12, border: "1px solid #efeeec", borderRadius: 10, background: "#fcfbfa" }}>
              <strong style={{ fontSize: 11 }}>Your account</strong>
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