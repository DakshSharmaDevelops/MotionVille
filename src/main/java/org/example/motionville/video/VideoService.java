package org.example.motionville.video;

import java.util.Locale;
import java.util.Optional;
import java.io.IOException;
import org.example.motionville.channel.Channel;
import org.example.motionville.channel.ChannelRepository;
import org.example.motionville.channel.ResourceNotFoundException;
import org.example.motionville.video.web.VideoUploadForm;
import org.example.motionville.video.web.VideoEditForm;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {
    private static final long MAX_UPLOAD_BYTES = 500L * 1024 * 1024;
    private final VideoRepository videos;
    private final ChannelRepository channels;
    private final LocalMediaStorage storage;

    public VideoService(VideoRepository videos, ChannelRepository channels, LocalMediaStorage storage) {
        this.videos = videos;
        this.channels = channels;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Page<Video> findPublicVideos(String search, String category, Pageable pageable) {
        String query = search == null ? "" : search.trim();
        return videos.searchPublic(query, category == null ? "" : category.trim(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> findPublicTrending(Pageable pageable) {
        return videos.findPublicTrending(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> findChannelVideos(Long channelId, Pageable pageable) {
        return videos.findByChannelIdAndDeletedFalseAndVisibilityOrderByCreatedAtDesc(
                channelId, VideoVisibility.PUBLIC, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Video> findVideosFromChannels(java.util.Collection<Long> channelIds, Pageable pageable) {
        if (channelIds.isEmpty()) {
            return Page.empty(pageable);
        }
        return videos.findByChannelIdInAndDeletedFalseAndVisibilityOrderByCreatedAtDesc(
                channelIds, VideoVisibility.PUBLIC, pageable);
    }

    @Transactional
    public Video upload(Long ownerId, VideoUploadForm form) {
        Channel channel = channels.findByOwnerId(ownerId)
                .orElseThrow(() -> new ChannelMissingException("Create a channel before uploading videos."));
        MultipartFile file = form.getFile();
        validateUpload(file);
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String storageKey = storage.store(file, contentType);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storage.delete(storageKey);
                }
            }
        });
        try {
            return videos.save(new Video(
                    channel,
                    form.getTitle().trim(),
                    trimOrEmpty(form.getDescription()),
                    trimOrEmpty(form.getCategory()),
                    trimOrEmpty(form.getTags()),
                    storageKey,
                    contentType,
                    form.getVisibility() == null ? VideoVisibility.PUBLIC : form.getVisibility()));
        } catch (RuntimeException exception) {
            try {
                storage.delete(storageKey);
            } catch (MediaStorageException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    @Transactional
    public Video view(Long videoId, Long viewerId) {
        Video video = get(videoId);
        if (video.getVisibility() == VideoVisibility.PRIVATE
                && (viewerId == null || !video.getChannel().getOwner().getId().equals(viewerId))) {
            throw new AccessDeniedException("This video is private.");
        }
        videos.incrementViewCount(videoId);
        return get(videoId);
    }

    @Transactional(readOnly = true)
    public Video get(Long videoId) {
        Video video = videos.findById(videoId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found."));
        return video;
    }

    @Transactional(readOnly = true)
    public Video getForOwner(Long videoId, Long ownerId) {
        Video video = get(videoId);
        assertOwner(video, ownerId);
        return video;
    }

    @Transactional
    public Video update(Long videoId, Long ownerId, VideoEditForm form) {
        Video video = get(videoId);
        assertOwner(video, ownerId);
        video.update(
                form.title().trim(),
                trimOrEmpty(form.description()),
                trimOrEmpty(form.category()),
                trimOrEmpty(form.tags()),
                form.visibility());
        return video;
    }

    @Transactional(readOnly = true)
    public Optional<Video> findByStorageKey(String storageKey) {
        return videos.findByStorageKeyAndDeletedFalse(storageKey);
    }

    @Transactional(readOnly = true)
    public Resource loadMedia(Video video, Long viewerId) {
        if (video.getVisibility() == VideoVisibility.PRIVATE
                && (viewerId == null || !video.getChannel().getOwner().getId().equals(viewerId))) {
            throw new AccessDeniedException("This video is private.");
        }
        return storage.load(video.getStorageKey());
    }

    @Transactional
    public void softDelete(Long videoId, Long ownerId) {
        Video video = get(videoId);
        assertOwner(video, ownerId);
        video.softDelete();
    }

    private void assertOwner(Video video, Long ownerId) {
        if (!video.getChannel().getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Only the channel owner can manage this video.");
        }
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidVideoException("Choose a video file to upload.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new InvalidVideoException("Video uploads must be 500 MB or smaller.");
        }
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!contentType.equals("video/mp4")
                && !contentType.equals("video/webm")
                && !contentType.equals("video/quicktime")) {
            throw new InvalidVideoException("Only MP4, WebM, and MOV videos are supported.");
        }
        try (var input = file.getInputStream()) {
            byte[] signature = input.readNBytes(12);
            boolean webm = signature.length >= 4
                    && signature[0] == 0x1A && signature[1] == 0x45
                    && signature[2] == (byte) 0xDF && signature[3] == (byte) 0xA3;
            boolean isoBmff = signature.length >= 8
                    && signature[4] == 'f' && signature[5] == 't'
                    && signature[6] == 'y' && signature[7] == 'p';
            boolean signatureMatches = contentType.equals("video/webm") ? webm : isoBmff;
            if (!signatureMatches) {
                throw new InvalidVideoException("The file does not have a supported video signature.");
            }
        } catch (IOException exception) {
            throw new InvalidVideoException("The uploaded video could not be read.");
        }
    }

    private String trimOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static class ChannelMissingException extends RuntimeException {
        public ChannelMissingException(String message) {
            super(message);
        }
    }

    public static class InvalidVideoException extends RuntimeException {
        public InvalidVideoException(String message) {
            super(message);
        }
    }
}
