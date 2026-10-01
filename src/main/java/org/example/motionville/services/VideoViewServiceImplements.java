package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.VideoViewRequest;
import org.example.motionville.dto.VideoViewResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.engagement.VideoView;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.engagement.VideoViewRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional
public class VideoViewServiceImplements implements VideoViewService {

    private static final int DEFAULT_VIEW_THRESHOLD_SECONDS = 30;

    private final VideoViewRepository videoViewRepository;
    private final VideoRepository videoRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public VideoViewResponse recordView(Long videoId, VideoViewRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> notFound("Video", videoId));
        if (video.getProcessingStatus() != VideoProcessingStatus.READY
                && video.getProcessingStatus() != VideoProcessingStatus.UPLOADED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Video is not ready for playback");
        }

        int requiredSeconds = requiredWatchSeconds(video.getDurationSeconds());
        int watchedSeconds = request.getWatchedSeconds();
        if (video.getDurationSeconds() != null && video.getDurationSeconds() > 0
                && watchedSeconds > video.getDurationSeconds()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Watched seconds cannot exceed the video duration");
        }

        if (watchedSeconds < requiredSeconds) {
            return new VideoViewResponse(false, requiredSeconds, null, videoId,
                    request.getViewerId(), null);
        }

        AppUser viewer = request.getViewerId() == null ? null
                : appUserRepository.findById(request.getViewerId())
                    .orElseThrow(() -> notFound("User", request.getViewerId()));
        Instant viewedAt = Instant.now();
        VideoView event = VideoView.builder()
                .video(video)
                .viewer(viewer)
                .viewedAt(viewedAt)
                .build();
        VideoView saved = videoViewRepository.save(event);
        return new VideoViewResponse(true, requiredSeconds, saved.getId(), videoId,
                viewer == null ? null : viewer.getId(), saved.getViewedAt());
    }

    private int requiredWatchSeconds(Integer durationSeconds) {
        if (durationSeconds != null && durationSeconds > 0 && durationSeconds < 30) {
            return (durationSeconds + 1) / 2;
        }
        return DEFAULT_VIEW_THRESHOLD_SECONDS;
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }
}
