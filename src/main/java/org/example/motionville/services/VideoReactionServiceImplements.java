package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.VideoReactionResponse;
import org.example.motionville.dto.VideoReactionSummary;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.engagement.VideoReaction;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.engagement.VideoReactionRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VideoReactionServiceImplements implements VideoReactionService {

    private final VideoReactionRepository videoReactionRepository;
    private final VideoRepository videoRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationCreationService notificationCreationService;

    @Override
    @Transactional(readOnly = true)
    public VideoReactionSummary getVideoReactionSummary(Long videoId, Long userId) {
        requireVideo(videoId);
        ReactionType userReaction = userId == null ? null
                : videoReactionRepository.findByVideo_VideoIdAndUser_Id(videoId, userId)
                    .map(VideoReaction::getReaction).orElse(null);
        return new VideoReactionSummary(
                videoReactionRepository.countByVideo_VideoIdAndReaction(videoId, ReactionType.LIKE),
                videoReactionRepository.countByVideo_VideoIdAndReaction(videoId, ReactionType.DISLIKE),
                userReaction);
    }

    @Override
    @Transactional
    public VideoReactionResponse setReaction(Long videoId, Long userId, ReactionType reactionType) {
        Video video = requireVideo(videoId);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> notFound("User", userId));

        VideoReaction reaction = videoReactionRepository
                .findByVideo_VideoIdAndUser_Id(videoId, userId)
                .orElseGet(() -> VideoReaction.builder().video(video).user(user).build());
        ReactionType previousReaction = reaction.getReaction();
        reaction.setVideo(video);
        reaction.setUser(user);
        reaction.setReaction(reactionType);
        VideoReaction saved = videoReactionRepository.save(reaction);
        if (previousReaction != reactionType) {
            notificationCreationService.notifyVideoReaction(video, user, reactionType);
        }
        return new VideoReactionResponse(videoId, userId, saved.getReaction());
    }

    @Override
    @Transactional
    public void deleteReaction(Long videoId, Long userId) {
        requireVideo(videoId);
        VideoReaction reaction = videoReactionRepository
                .findByVideo_VideoIdAndUser_Id(videoId, userId)
                .orElseThrow(() -> notFound("Reaction", videoId));
        videoReactionRepository.delete(reaction);
    }

    private Video requireVideo(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> notFound("Video", videoId));
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }
}
