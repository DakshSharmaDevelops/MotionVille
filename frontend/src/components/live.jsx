import { useEffect, useRef, useState } from "react";
import { apiRequest } from "../api/videoApi.js";
import { Modal, Icon } from "./ui.jsx";

function useBroadcast(id) {
  const [broadcast, setBroadcast] = useState(null);
  const [error, setError] = useState("");
  useEffect(() => {
    if (!id) return undefined;
    let active = true;
    let timer;
    const controller = new AbortController();
    async function poll() {
      try {
        const result = await apiRequest(`/live/${id}`, { signal: controller.signal });
        if (active) { setBroadcast(result); setError(""); }
      } catch (failure) {
        if (active) { setError(failure.message); setBroadcast(null); }
      } finally {
        if (active) timer = setTimeout(poll, 3000);
      }
    }
    poll();
    return () => { active = false; clearTimeout(timer); controller.abort(); };
  }, [id]);
  return { broadcast, error };
}

function LivePlayer({ broadcast }) {
  const playerRef = useRef(null);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const url = broadcast?.status === "LIVE" ? broadcast.playbackUrl : null;
  useEffect(() => {
    const player = playerRef.current;
    if (!url || !player) return undefined;
    let active = true;
    let hls;
    setError("");
    // Safari supports HLS natively; other browsers use MediaSource via hls.js.
    if (player.canPlayType("application/vnd.apple.mpegurl")) {
      player.src = url;
      player.play().catch(() => {}); // A browser may require pressing Play.
    } else {
      import("hls.js").then(({ default: Hls }) => {
        if (!active) return;
        if (!Hls.isSupported()) { setError("This browser cannot play live HLS video."); return; }
        hls = new Hls();
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) setError("The live connection was interrupted. Retry playback.");
        });
        hls.on(Hls.Events.MANIFEST_PARSED, () => player.play().catch(() => {}));
        hls.loadSource(url);
        hls.attachMedia(player);
      }).catch(() => { if (active) setError("The live player could not load."); });
    }
    return () => {
      active = false; hls?.destroy(); player.pause();
      player.removeAttribute("src"); player.load();
    };
  }, [url, attempt]);
  if (!url) return <p role="status">Waiting for the broadcaster to connect…</p>;
  return <div>
    <video className="live-player" ref={playerRef} controls autoPlay muted playsInline
      onError={() => setError("The live connection was interrupted. Retry playback.")} />
    <small>Live audio starts muted. Use the player controls to unmute.</small>
    {error && <p role="alert">{error} <button type="button" onClick={() => setAttempt(value => value + 1)}>Retry</button></p>}
  </div>;
}

export function LiveStudio({ channel, user, onClose }) {
  const [title, setTitle] = useState(`${channel?.name || "My channel"} live`);
  const [password, setPassword] = useState("");
  const [studio, setStudio] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const { broadcast, error: statusError } = useBroadcast(studio?.broadcast.id);
  const viewerUrl = studio ? `${window.location.origin}/?live=${studio.broadcast.id}` : "";

  async function start(event) {
    event.preventDefault();
    setBusy(true); setError("");
    try {
      const result = await apiRequest("/live", { method: "POST", body: JSON.stringify({
        channelId: channel.channelId, title: title.trim(), username: user.username, password,
      }) });
      setStudio(result); setPassword("");
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  async function end() {
    setBusy(true); setError("");
    try {
      await apiRequest(`/live/${studio.broadcast.id}`, {
        method: "DELETE", headers: { "X-Live-Token": studio.managementToken },
      });
      setStudio(null);
    } catch (failure) { setError(failure.message); }
    finally { setBusy(false); }
  }
  return <Modal onClose={onClose}>
    <section className="dialog create-dialog live-dialog" role="dialog" aria-modal="true" aria-labelledby="live-studio-title">
      <div className="dialog-topline"><span>Your live studio</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
      <h2 id="live-studio-title">Broadcast from {channel?.name}</h2>
      {!studio ? <form className="dialog-form" onSubmit={start}>
        <p>Create a broadcast, then connect OBS to share your camera, screen, or microphone. Reopen an existing broadcast with the same account.</p>
        <label>Broadcast title<input value={title} onChange={e => setTitle(e.target.value)} required maxLength={150} /></label>
        <label>Confirm your account password<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
        <button className="button button-primary" disabled={busy || !channel || !user || !title.trim()}>{busy ? "Opening studio…" : "Create / reopen broadcast"}</button>
      </form> : <div className="dialog-form">
        <p role="status">{broadcast?.status === "LIVE" ? "● Live now" : "Waiting for OBS"}</p>
        <p>In OBS, choose Settings → Stream → Custom. Paste these values, use H.264 video with AAC audio and a 2-second keyframe interval, then click Start Streaming.</p>
        <label>Server URL<input readOnly value={studio.serverUrl} onFocus={e => e.target.select()} /></label>
        <label>Stream key — keep private<input type="password" readOnly value={studio.streamKey} onFocus={e => e.target.select()} /></label>
        <button type="button" onClick={async () => {
          try { await navigator.clipboard.writeText(studio.streamKey); }
          catch { setError("Select the stream key and copy it manually."); }
        }}>Copy stream key</button>
        <label>Viewer link<input readOnly value={viewerUrl} onFocus={e => e.target.select()} /></label>
        <LivePlayer broadcast={broadcast} />
        <p>Closing this window keeps the broadcast running. End it below when finished.</p>
        <button className="button button-primary" disabled={busy} onClick={end}>{busy ? "Ending…" : "End broadcast"}</button>
      </div>}
      {(error || (studio && statusError)) && <p className="inline-error" role="alert">{error || statusError}</p>}
    </section>
  </Modal>;
}

export function LiveWatchDialog({ id, onClose }) {
  const { broadcast, error } = useBroadcast(id);
  return <Modal onClose={onClose}><section className="dialog live-dialog" role="dialog" aria-modal="true" aria-labelledby="live-watch-title">
    <div className="dialog-topline"><span>Live broadcast</span><button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button></div>
    <h2 id="live-watch-title">{broadcast?.title || "Live broadcast"}</h2>
    {broadcast && <p>{broadcast.channelName}</p>}
    {error ? <p role="alert">{error}</p> : <LivePlayer broadcast={broadcast} />}
  </section></Modal>;
}

export function LiveBroadcastList({ onWatch }) {
  const [items, setItems] = useState([]);
  useEffect(() => {
    let active = true;
    let timer;
    const controller = new AbortController();
    async function poll() {
      try { const data = await apiRequest("/live", { signal: controller.signal }); if (active) setItems(data); }
      catch { if (active) setItems([]); }
      finally { if (active) timer = setTimeout(poll, 10000); }
    }
    poll();
    return () => { active = false; clearTimeout(timer); controller.abort(); };
  }, []);
  if (!items.length) return null;
  return <section className="live-broadcast-list" aria-label="Live now">
    <h2>Live now</h2>
    {items.map(item => <button key={item.id} className="action-pill" onClick={() => onWatch(item.id)}>
      <span className="live-status-dot" /> {item.title} · {item.channelName}
    </button>)}
  </section>;
}
