import { useState } from "react";
import { formatDuration } from "../utils/format.js";
import { Icon, Modal } from "./ui.jsx";

export function PlaylistDialog({ playlist, onClose, onSave }) {
  const [title, setTitle] = useState(playlist?.title || "");
  const [description, setDescription] = useState(playlist?.description || "");
  const [visibility, setVisibility] = useState(playlist?.visibility || "PRIVATE");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    if (!title.trim()) {
      setError("Enter a playlist title.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      await onSave({ title: title.trim(), description: description.trim(), visibility });
    } catch (saveError) {
      setError(saveError.message || "Could not save the playlist.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="playlist-dialog-title">
        <div className="dialog-topline">
          <span className="dialog-step"><Icon name="library" size={15} /> Your library</span>
          <button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </div>
        <h2 id="playlist-dialog-title">{playlist ? "Edit playlist" : "Create playlist"}</h2>
        <p className="dialog-subtitle">Organize videos you want to watch or revisit.</p>
        <form className="dialog-form" onSubmit={submit}>
          <label>Title<input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={150} required autoFocus /></label>
          <label>Description <span className="optional">Optional</span><textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={3} /></label>
          <label>Visibility<select value={visibility} onChange={(event) => setVisibility(event.target.value)}>
            <option value="PRIVATE">Private</option>
            <option value="UNLISTED">Unlisted</option>
            <option value="PUBLIC">Public</option>
          </select></label>
          {error && <p className="inline-error" role="alert">{error}</p>}
          <div className="dialog-actions">
            <button type="button" className="text-button" onClick={onClose} disabled={busy}>Cancel</button>
            <button className="button button-primary" type="submit" disabled={busy}>{busy ? "Saving…" : playlist ? "Save changes" : "Create playlist"}</button>
          </div>
        </form>
      </section>
    </Modal>
  );
}

export function SaveToPlaylistDialog({ video, playlists, onClose, onSave, onCreatePlaylist }) {
  const [error, setError] = useState("");
  const [busyId, setBusyId] = useState(null);

  async function save(playlist) {
    setBusyId(playlist.id);
    setError("");
    try {
      await onSave(playlist.id);
    } catch (saveError) {
      setError(saveError.message || "Could not add this video to the playlist.");
    } finally {
      setBusyId(null);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="save-playlist-title">
        <div className="dialog-topline">
          <span className="dialog-step"><Icon name="library" size={15} /> Your library</span>
          <button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </div>
        <h2 id="save-playlist-title">Save to playlist</h2>
        <p className="dialog-subtitle">{video.title}</p>
        <div className="playlist-picker-list">
          {playlists.map((playlist) => (
            <button className="playlist-picker-item" type="button" key={playlist.id} onClick={() => save(playlist)} disabled={busyId !== null || playlist.videoIds?.some((id) => Number(id) === Number(video.videoId))}>
              <span><strong>{playlist.title}</strong><small>{playlist.videoIds?.length || 0} videos · {playlist.visibility.toLowerCase()}</small></span>
              <span>{playlist.videoIds?.some((id) => Number(id) === Number(video.videoId)) ? "Saved" : busyId === playlist.id ? "Saving…" : <Icon name="plus" size={17} />}</span>
            </button>
          ))}
          {!playlists.length && <p className="playlist-empty-note">Create a playlist to save this video.</p>}
        </div>
        {error && <p className="inline-error" role="alert">{error}</p>}
        <div className="dialog-actions">
          <button type="button" className="text-button" onClick={onClose}>Cancel</button>
          <button type="button" className="button button-primary" onClick={onCreatePlaylist}>Create playlist</button>
        </div>
      </section>
    </Modal>
  );
}

export function AddVideosToPlaylistDialog({ playlist, videos, loading, loadError, onClose, onAdd }) {
  const [search, setSearch] = useState("");
  const [addingId, setAddingId] = useState(null);
  const [addedIds, setAddedIds] = useState(() => new Set((playlist.videoIds || []).map(Number)));
  const [error, setError] = useState("");
  const availableVideos = videos.filter((video) => {
    if (!video.serverVideo || addedIds.has(Number(video.videoId))) return false;
    return `${video.title} ${video.description || ""}`.toLowerCase().includes(search.trim().toLowerCase());
  });

  async function add(video) {
    setAddingId(video.videoId);
    setError("");
    try {
      await onAdd(video);
      setAddedIds((current) => new Set(current).add(Number(video.videoId)));
    } catch (addError) {
      setError(addError.message || "Could not add the video.");
    } finally {
      setAddingId(null);
    }
  }

  return (
    <Modal onClose={onClose}>
      <section className="dialog create-dialog" role="dialog" aria-modal="true" aria-labelledby="add-playlist-videos-title">
        <div className="dialog-topline">
          <span className="dialog-step"><Icon name="library" size={15} /> {playlist.title}</span>
          <button className="icon-button" onClick={onClose} aria-label="Close"><Icon name="close" /></button>
        </div>
        <h2 id="add-playlist-videos-title">Add videos</h2>
        <p className="dialog-subtitle">Choose from videos currently loaded in your feed.</p>
        <label className="playlist-video-search">
          <span className="visually-hidden">Search videos</span>
          <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search videos" />
        </label>
        <div className="playlist-picker-list">
          {availableVideos.map((video) => (
            <div className="playlist-picker-item" key={video.videoId}>
              <span><strong>{video.title}</strong><small>{video.description || "Video"}</small></span>
              <button type="button" className="video-card-action" onClick={() => add(video)} disabled={addingId !== null}>
                {addingId === video.videoId ? "Adding…" : "Add"}
              </button>
            </div>
          ))}
          {loading && <p className="playlist-empty-note" role="status">Loading videos…</p>}
          {!loading && !availableVideos.length && <p className="playlist-empty-note">{videos.length ? "No matching videos available to add." : "No videos are available to add yet."}</p>}
        </div>
        {(error || loadError) && <p className="inline-error" role="alert">{error || loadError}</p>}
        <div className="dialog-actions"><button type="button" className="button button-primary" onClick={onClose}>Done</button></div>
      </section>
    </Modal>
  );
}

export function PlaylistLibrary({ playlists, selectedPlaylist, videos, loading, error, onCreate, onOpen, onBack, onEdit, onDelete, onSelectVideo, onRemoveVideo, onMoveVideo, onAddVideos }) {
  return (
    <section className="playlist-library">
      <div className="feed-heading">
        <div>
          <p className="section-eyebrow">Your library</p>
          <h2>{selectedPlaylist?.title || "Playlists"}</h2>
          {selectedPlaylist?.description && <p className="dialog-subtitle">{selectedPlaylist.description}</p>}
        </div>
        <div className="feed-controls">
          {selectedPlaylist
            ? <>
                <button className="video-card-action" onClick={onAddVideos}>Add videos</button>
                <button className="video-card-action" onClick={() => onEdit(selectedPlaylist)}>Edit playlist</button>
                <button className="video-card-action video-card-delete" onClick={() => onDelete(selectedPlaylist)}>Delete playlist</button>
                <button className="feed-filter" onClick={onBack}>All playlists</button>
              </>
            : <button className="button button-primary" onClick={onCreate}><Icon name="plus" size={16} /> Create playlist</button>}
        </div>
      </div>
      {error && <p className="feed-error" role="alert">{error}</p>}
      {loading ? <p role="status">Loading playlists…</p> : selectedPlaylist ? (
        videos.length ? <div className="playlist-video-list">
          {videos.map((video, index) => (
            <article className="playlist-video-row" key={video.videoId}>
              <button className="playlist-video-open" onClick={() => onSelectVideo(video)}>
                {video.thumbnailUrl ? <img src={video.thumbnailUrl} alt="" /> : <span className="playlist-video-placeholder"><Icon name="video" /></span>}
                <span className="playlist-video-details"><strong>{video.title}</strong><small>{formatDuration(video.durationSeconds)} · {video.processingStatus || "Video"}</small></span>
              </button>
              <div className="playlist-row-actions">
                <button className="video-card-action" onClick={() => onMoveVideo(index, -1)} disabled={index === 0} aria-label="Move video up">↑</button>
                <button className="video-card-action" onClick={() => onMoveVideo(index, 1)} disabled={index === videos.length - 1} aria-label="Move video down">↓</button>
                <button className="video-card-action video-card-delete" onClick={() => onRemoveVideo(video)}>Remove</button>
              </div>
            </article>
          ))}
        </div> : <div className="empty-feed"><span><Icon name="video" size={25} /></span><h2>This playlist is empty</h2><p>Save videos to this playlist from a video card or while watching.</p></div>
      ) : playlists.length ? (
        <div className="playlist-grid">
          {playlists.map((playlist) => (
            <article className="playlist-card" key={playlist.id}>
              <button className="playlist-card-open" onClick={() => onOpen(playlist)}>
                <span className="playlist-card-art"><Icon name="library" size={25} /><span>{playlist.videoIds?.length || 0} videos</span></span>
                <strong>{playlist.title}</strong><small>{playlist.visibility.toLowerCase()}</small>
              </button>
            </article>
          ))}
        </div>
      ) : <div className="empty-feed"><span><Icon name="library" size={25} /></span><h2>No playlists yet</h2><p>Create playlists to collect and organize videos for later.</p><button className="button button-primary" onClick={onCreate}><Icon name="plus" size={16} /> Create playlist</button></div>}
    </section>
  );
}
