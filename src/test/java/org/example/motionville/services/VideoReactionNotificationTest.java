package org.example.motionville.services;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.engagement.VideoReaction;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.engagement.VideoReactionRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoReactionNotificationTest {

    private VideoReactionRepository reactions;
    private VideoRepository videos;
    private AppUserRepository users;
    private NotificationCreationService notificationCreationService;
    private VideoReactionServiceImplements service;
    private Video video;
    private AppUser actor;

    @BeforeEach
    void setUp() {
        reactions = mock(VideoReactionRepository.class);
        videos = mock(VideoRepository.class);
        users = mock(AppUserRepository.class);
        notificationCreationService = mock(NotificationCreationService.class);
        service = new VideoReactionServiceImplements(
                reactions, videos, users, notificationCreationService);

        actor = user(2L);
        AppUser owner = user(1L);
        Channel channel = new Channel();
        channel.setChannelId(3L);
        channel.setOwner(owner);
        video = new Video();
        video.setVideoId(4L);
        video.setTitle("Test video");
        video.setChannel(channel);

        when(videos.findById(4L)).thenReturn(Optional.of(video));
        when(users.findById(2L)).thenReturn(Optional.of(actor));
        when(reactions.save(any(VideoReaction.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void notifiesOwnerOnNewLike() {
        when(reactions.findByVideo_VideoIdAndUser_Id(4L, 2L)).thenReturn(Optional.empty());

        service.setReaction(4L, 2L, ReactionType.LIKE);

        verify(notificationCreationService).notifyVideoReaction(video, actor, ReactionType.LIKE);
    }

    @Test
    void notifiesOwnerWhenReactionChangesFromLikeToDislike() {
        VideoReaction existing = VideoReaction.builder()
                .video(video)
                .user(actor)
                .reaction(ReactionType.LIKE)
                .build();
        when(reactions.findByVideo_VideoIdAndUser_Id(4L, 2L)).thenReturn(Optional.of(existing));

        service.setReaction(4L, 2L, ReactionType.DISLIKE);

        verify(notificationCreationService).notifyVideoReaction(video, actor, ReactionType.DISLIKE);
    }

    @Test
    void doesNotCreateDuplicateNotificationWhenReactionIsUnchanged() {
        VideoReaction existing = VideoReaction.builder()
                .video(video)
                .user(actor)
                .reaction(ReactionType.LIKE)
                .build();
        when(reactions.findByVideo_VideoIdAndUser_Id(4L, 2L)).thenReturn(Optional.of(existing));

        service.setReaction(4L, 2L, ReactionType.LIKE);

        verifyNoInteractions(notificationCreationService);
    }

    private static AppUser user(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setDisplayName("User " + id);
        return user;
    }
}
