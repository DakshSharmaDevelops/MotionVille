package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.WatchHistoryResponse;
import org.example.motionville.dto.WatchHistoryUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.engagement.WatchHistory;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class WatchHistoryServiceImplements implements WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final AppUserRepository appUserRepository;
    private final VideoRepository videoRepository;

    @Override
    public WatchHistoryResponse recordProgress(Long videoId, WatchHistoryUpdateRequest request) {
        AppUser user = findUser(request.getUserId());
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> notFound("Video", videoId));

        if (video.getVisibility() == VideoVisibility.PRIVATE
                && !video.getChannel().getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot watch private video");
        }

        if (video.getDurationSeconds() != null
                && video.getDurationSeconds() > 0
                && request.getLastPositionSeconds() > video.getDurationSeconds()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Watch position cannot exceed the video duration");
        }

        WatchHistory history = watchHistoryRepository
                .findByUser_IdAndVideo_VideoId(user.getId(), videoId)
                .orElseGet(() -> WatchHistory.builder().user(user).video(video).build());
        history.setUser(user);
        history.setVideo(video);
        history.setLastPositionSeconds(request.getLastPositionSeconds());
        history.setLastWatchedAt(Instant.now());
        return toResponse(watchHistoryRepository.save(history));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WatchHistoryResponse> getHistory(Long userId) {
        findUser(userId);
        return watchHistoryRepository.findByUser_IdOrderByLastWatchedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void removeHistoryItem(Long userId, Long videoId) {
        findUser(userId);
        WatchHistory history = watchHistoryRepository
                .findByUser_IdAndVideo_VideoId(userId, videoId)
                .orElseThrow(() -> notFound("Watch history item", videoId));
        watchHistoryRepository.delete(history);
    }

    @Override
    public void clearHistory(Long userId) {
        findUser(userId);
        watchHistoryRepository.deleteAllByUser_Id(userId);
    }

    private AppUser findUser(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> notFound("User", userId));
    }

    private WatchHistoryResponse toResponse(WatchHistory history) {
        Video video = history.getVideo();
        return new WatchHistoryResponse(
                history.getId(),
                history.getUser().getId(),
                video.getVideoId(),
                video.getTitle(),
                video.getThumbnailUrl(),
                video.getDurationSeconds(),
                history.getLastPositionSeconds(),
                history.getLastWatchedAt());
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }
}
