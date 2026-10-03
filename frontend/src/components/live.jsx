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

function waitForIceGathering(peer) {
  if (peer.iceGatheringState === "complete") return Promise.resolve();
  return new Promise((resolve, reject) => {
    const timeout = window.setTimeout(() => {
      peer.removeEventListener("icegatheringstatechange", onStateChange);
      reject(new Error("Network setup timed out. Check the WebRTC connection and try again."));
    }, 15000);
    function onStateChange() {
      if (peer.iceGatheringState === "complete") {
        window.clearTimeout(timeout);
        peer.removeEventListener("icegatheringstatechange", onStateChange);
        resolve();
      }
    }
    peer.addEventListener("icegatheringstatechange", onStateChange);
  });
}

function WebRtcBroadcaster({ studio }) {
  const previewRef = useRef(null);
  const peerRef = useRef(null);
  const streamRef = useRef(null);
  const resourceUrlRef = useRef(null);
  const [publishing, setPublishing] = useState(false);
  const [connecting, setConnecting] = useState(false);
  const [error, setError] = useState("");

  const authorization = `Basic ${btoa(`${studio.publishUsername}:${studio.publishPassword}`)}`;

  function releaseMedia() {
    peerRef.current?.close();
    peerRef.current = null;
    streamRef.current?.getTracks().forEach(track => track.stop());
    streamRef.current = null;
    if (previewRef.current) previewRef.current.srcObject = null;
  }

  async function stopPublishing() {
    const resourceUrl = resourceUrlRef.current;
    resourceUrlRef.current = null;
    releaseMedia();
    setPublishing(false);
    setConnecting(false);
    if (resourceUrl) {
      const response = await fetch(resourceUrl, {
        method: "DELETE",
        headers: { Authorization: authorization },
      });
      if (!response.ok && response.status !== 404) {
        throw new Error(`The stream stopped locally, but MediaMTX could not close the publishing session (${response.status}).`);
      }
    }
  }

  useEffect(() => () => {
    const resourceUrl = resourceUrlRef.current;
    releaseMedia();
    if (resourceUrl) {
      fetch(resourceUrl, {
        method: "DELETE",
        headers: { Authorization: authorization },
      }).catch(failure => console.error("Could not close the MediaMTX publishing session:", failure));
    }
  }, [authorization]);

  async function startPublishing(source) {
    setError("");
    setConnecting(true);
    try {
      const captureMedia = source === "screen"
        ? navigator.mediaDevices?.getDisplayMedia
        : navigator.mediaDevices?.getUserMedia;
      if (!captureMedia || !window.RTCPeerConnection) {
        throw new Error("Browser streaming requires a modern browser on localhost or HTTPS.");
      }
      const stream = source === "screen"
        ? await navigator.mediaDevices.getDisplayMedia({ video: true, audio: true })
        : await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
      streamRef.current = stream;
      if (previewRef.current) previewRef.current.srcObject = stream;

      const peer = new RTCPeerConnection();
      peerRef.current = peer;
      peer.onconnectionstatechange = () => {
        if (peer.connectionState === "failed") {
          setError("The WebRTC connection failed. Check that MediaMTX is running and try again.");
          stopPublishing().catch(failure => setError(failure.message));
        }
      };
      stream.getTracks().forEach(track => peer.addTrack(track, stream));
      const videoTransceiver = peer.getTransceivers().find(transceiver => transceiver.sender.track?.kind === "video");
      const browserCodecs = window.RTCRtpSender?.getCapabilities?.("video")?.codecs || [];
      const hlsCompatibleCodecs = [
        ...browserCodecs.filter(codec => /video\/h264/i.test(codec.mimeType)),
        ...browserCodecs.filter(codec => /video\/vp9/i.test(codec.mimeType)),
      ];
      if (!videoTransceiver?.setCodecPreferences || hlsCompatibleCodecs.length === 0) {
        throw new Error("This browser has no supported HLS-compatible WebRTC video codec.");
      }
      videoTransceiver.setCodecPreferences(hlsCompatibleCodecs);
      const offer = await peer.createOffer();
      await peer.setLocalDescription(offer);
      await waitForIceGathering(peer);

      const response = await fetch(studio.whipUrl, {
        method: "POST",
        headers: {
          Authorization: authorization,
          "Content-Type": "application/sdp",
          Accept: "application/sdp",
        },
        body: peer.localDescription.sdp,
      });
      if (!response.ok) {
        const detail = await response.text();
        throw new Error(detail || `MediaMTX rejected the stream (${response.status}).`);
      }
      const location = response.headers.get("Location");
      if (location) resourceUrlRef.current = new URL(location, studio.whipUrl).toString();
      const answer = await response.text();
      await peer.setRemoteDescription({ type: "answer", sdp: answer });
      setPublishing(true);

      const videoTrack = stream.getVideoTracks()[0];
      if (source === "screen" && videoTrack) {
        videoTrack.addEventListener("ended", () => {
          stopPublishing().catch(failure => setError(failure.message));
        }, { once: true });
      }
    } catch (failure) {
      const resourceUrl = resourceUrlRef.current;
      resourceUrlRef.current = null;
      releaseMedia();
      if (resourceUrl) {
        try {
          await fetch(resourceUrl, {
            method: "DELETE",
            headers: { Authorization: authorization },
          });
        } catch (cleanupFailure) {
          console.error("Could not close the failed MediaMTX publishing session:", cleanupFailure);
        }
      }
      setError(failure.name === "NotAllowedError"
        ? "Camera or screen access was cancelled. Allow access in your browser and try again."
        : failure.message || "Could not start the live stream.");
    } finally {
      setConnecting(false);
    }
  }

  return <div className="live-publisher">
    {publishing || connecting
      ? <video className="live-preview" ref={previewRef} autoPlay muted playsInline />
      : <p>Choose what to share. Your browser may ask for camera, microphone, or screen permission.</p>}
    {!publishing && <div className="live-publisher-actions">
      <button className="button button-primary" type="button" disabled={connecting}
        onClick={() => startPublishing("camera")}>
        {connecting ? "Connecting…" : "Go live with camera"}
      </button>
      <button className="button" type="button" disabled={connecting}
        onClick={() => startPublishing("screen")}>
        Share screen
      </button>
    </div>}
    {publishing && <button className="button" type="button" onClick={() => {
      stopPublishing().catch(failure => setError(failure.message));
    }}>Stop sharing</button>}
    {error && <p className="inline-error" role="alert">{error}</p>}
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
        <p>Create a broadcast, then share your camera or screen directly from this browser. Reopen an existing broadcast with the same account.</p>
        <label>Broadcast title<input value={title} onChange={e => setTitle(e.target.value)} required maxLength={150} /></label>
        <label>Confirm your account password<input type="password" autoComplete="current-password" value={password} onChange={e => setPassword(e.target.value)} required /></label>
        <button className="button button-primary" disabled={busy || !channel || !user || !title.trim()}>{busy ? "Opening studio…" : "Create / reopen broadcast"}</button>
      </form> : <div className="dialog-form">
        <p role="status">{broadcast?.status === "LIVE" ? "● Live now" : "Waiting for you to start sharing"}</p>
        <WebRtcBroadcaster studio={studio} />
        <label>Viewer link<input readOnly value={viewerUrl} onFocus={e => e.target.select()} /></label>
        <LivePlayer broadcast={broadcast} />
        <p>Closing this studio stops browser capture. End the broadcast when you are finished.</p>
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
