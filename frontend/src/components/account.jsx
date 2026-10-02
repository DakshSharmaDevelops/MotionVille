import { useState } from "react";
import { Avatar, Icon, Modal } from "./ui.jsx";

export function AccountDialog({ mode, user, myChannels, subscriptionChannels, onClose, onRegister, onUpdate, onLogout, onCreateChannel, onShowSubscriptions }) {
  const registerMode = mode === "register";
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
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    if (!form.username.trim() || !form.email.trim() || !form.displayName.trim()) {
      setError("Username, email and display name are required.");
      return;
    }
    if (registerMode && !form.password) {
      setError("Password is required for registration.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      if (registerMode) await onRegister(form);
      else await onUpdate(form);
    } catch (requestError) {
      setError(requestError.message || "Could not save your profile.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="account-dialog-title">
        <div className="dialog-topline">
          <span className="dialog-step"><Icon name="sparkle" size={15} /> {registerMode ? "Join MotionVille" : "Your profile"}</span>
          <button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </div>
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 18 }}>
          <Avatar src={form.avatarUrl} name={form.displayName || form.username || "M"} size="large" />
          <div>
            <h2 id="account-dialog-title" style={{ marginBottom: 3 }}>{registerMode ? "Create your profile" : "Profile"}</h2>
            <p className="dialog-subtitle" style={{ marginBottom: 0 }}>{registerMode ? "Create the account used by your MotionVille frontend." : "Update your profile details and manage your MotionVille account."}</p>
          </div>
        </div>
        <form className="dialog-form" onSubmit={submit}>
          <div className="form-two-col">
            <label>Username<input name="username" value={form.username} onChange={update} maxLength="50" placeholder="yourusername" required /></label>
            <label>Display name<input name="displayName" value={form.displayName} onChange={update} maxLength="100" placeholder="Your name" required /></label>
          </div>
          <label>Email<input name="email" type="email" value={form.email} onChange={update} placeholder="you@example.com" required /></label>
          <label>Password <span className="optional">{registerMode ? "Required" : "Leave blank to keep current"}</span><input name="password" type="password" value={form.password} onChange={update} placeholder={registerMode ? "Create a password" : "New password"} /></label>
          <label>Avatar URL <span className="optional">Optional</span><input name="avatarUrl" type="url" value={form.avatarUrl} onChange={update} placeholder="https://example.com/avatar.jpg" /></label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          {!registerMode && (
            <div style={{ display: "grid", gap: 8, padding: 12, border: "1px solid #efeeec", borderRadius: 10, background: "#fcfbfa" }}>
              <strong style={{ fontSize: 11 }}>Your account</strong>
              <span style={{ color: "#85858d", fontSize: 10 }}>{myChannels.length} channel{myChannels.length === 1 ? "" : "s"} · {subscriptionChannels.length} subscription{subscriptionChannels.length === 1 ? "" : "s"}</span>
              <div style={{ display: "flex", gap: 7, flexWrap: "wrap" }}>
                <button type="button" className="feed-filter" onClick={onCreateChannel}>Create channel</button>
                <button type="button" className="feed-filter" onClick={onShowSubscriptions}>View subscriptions</button>
              </div>
            </div>
          )}
          <div className="dialog-actions">
            {!registerMode && <button type="button" className="text-button" onClick={onLogout} disabled={busy}>Sign out</button>}
            <button type="button" className="text-button" onClick={onClose} disabled={busy}>Cancel</button>
            <button className="button button-primary" type="submit" disabled={busy}>{busy ? "Saving…" : registerMode ? "Create profile" : "Save profile"} {!busy && <Icon name="chevron" size={17} />}</button>
          </div>
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
          <div className="empty-feed" style={{ minHeight: 180 }}><span><Icon name="subscriptions" size={25} /></span><h2>No subscribers yet</h2><p>Subscribers will appear here when people follow this channel.</p></div>
        )}
      </section>
    </Modal>
  );
}