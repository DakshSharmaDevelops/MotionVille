package org.example.motionville.services.notification;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.channel.Subscription;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.entity.notification.Notification;
import org.example.motionville.entity.notification.enums.NotificationType;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.channel.SubscriptionRepository;
import org.example.motionville.repo.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotificationCreationServiceTest {

    private NotificationRepository notifications;
    private SubscriptionRepository subscriptions;
    private org.example.motionville.services.account.EmailService emailService;
    private NotificationCreationService service;

    @BeforeEach
    void setUp() {
        notifications = mock(NotificationRepository.class);
        subscriptions = mock(SubscriptionRepository.class);
        emailService = mock(org.example.motionville.services.account.EmailService.class);
        service = new NotificationCreationService(notifications, subscriptions, emailService);
    }

    @Test
    void notifiesSubscribersWhenVideoIsPublished() {
        AppUser owner = user(1L, "Creator");
        AppUser subscriber = user(2L, "Viewer");
        Channel channel = channel(owner);
        Video video = video(channel);
        Subscription subscription = new Subscription();
        subscription.setSubscriber(subscriber);
        when(subscriptions.findByChannel_ChannelId(3L)).thenReturn(List.of(subscription));

        service.notifyNewVideo(video);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notifications).saveAll(captor.capture());
        Notification notification = captor.getValue().get(0);
        assertEquals(NotificationType.NEW_VIDEO, notification.getType());
        assertEquals(2L, notification.getRecipient().getId());
        assertEquals(1L, notification.getActor().getId());
        assertEquals(4L, notification.getVideo().getVideoId());
    }

    @Test
    void notifiesChannelOwnerWhenAnotherUserSubscribes() {
        AppUser owner = user(1L, "Creator");
        AppUser subscriber = user(2L, "Viewer");

        service.notifyNewSubscriber(channel(owner), subscriber);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.NEW_SUBSCRIBER, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getRecipient().getId());
        assertEquals(2L, captor.getValue().getActor().getId());
    }

    @Test
    void suppressesNewVideoNotificationToChannelOwner() {
        AppUser owner = user(1L, "Creator");
        Channel channel = channel(owner);
        Subscription ownSubscription = new Subscription();
        ownSubscription.setSubscriber(owner);
        when(subscriptions.findByChannel_ChannelId(3L)).thenReturn(List.of(ownSubscription));

        service.notifyNewVideo(video(channel));

        verify(notifications).saveAll(List.of());
    }

    @Test
    void notifiesVideoOwnerWhenAnotherUserComments() {
        AppUser owner = user(1L, "Creator");
        AppUser commenter = user(2L, "Viewer");
        Video video = video(channel(owner));
        Comment comment = new Comment();
        comment.setId(7L);
        comment.setVideo(video);
        comment.setAuthor(commenter);

        service.notifyNewComment(comment);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.NEW_COMMENT, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getRecipient().getId());
        assertEquals(7L, captor.getValue().getComment().getId());
    }

    @Test
    void notifiesParentCommentAuthorAboutReply() {
        AppUser parentAuthor = user(1L, "Creator");
        AppUser replier = user(2L, "Viewer");
        Video video = video(channel(parentAuthor));
        Comment parent = new Comment();
        parent.setAuthor(parentAuthor);
        Comment reply = new Comment();
        reply.setVideo(video);
        reply.setAuthor(replier);
        reply.setParentComment(parent);

        service.notifyCommentReply(reply);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.COMMENT_REPLY, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getRecipient().getId());
        assertEquals(2L, captor.getValue().getActor().getId());
    }

    @Test
    void suppressesNotificationsForSelfActions() {
        AppUser user = user(1L, "Creator");
        Video video = video(channel(user));
        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setAuthor(user);

        service.notifyNewComment(comment);

        verify(notifications, never()).save(any(Notification.class));
    }

    @Test
    void notifiesVideoOwnerWhenAnotherUserLikesVideo() {
        AppUser owner = user(1L, "Creator");
        AppUser liker = user(2L, "Viewer");
        Video video = video(channel(owner));

        service.notifyVideoReaction(video, liker, ReactionType.LIKE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.VIDEO_LIKE, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getRecipient().getId());
        assertEquals("Viewer liked your video: New video", captor.getValue().getMessage());
    }

    @Test
    void notifiesCommentAuthorWhenAnotherUserLikesComment() {
        AppUser author = user(1L, "Creator");
        AppUser liker = user(2L, "Viewer");
        Comment comment = new Comment();
        comment.setId(7L);
        comment.setAuthor(author);
        comment.setVideo(video(channel(author)));

        service.notifyCommentReaction(comment, liker, ReactionType.LIKE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.COMMENT_LIKE, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getRecipient().getId());
        assertEquals("Viewer liked your comment", captor.getValue().getMessage());
    }

    @Test
    void notifiesReplyAuthorWhenAnotherUserDislikesReply() {
        AppUser author = user(1L, "Creator");
        AppUser reactor = user(2L, "Viewer");
        Comment reply = new Comment();
        reply.setId(8L);
        reply.setAuthor(author);
        reply.setVideo(video(channel(author)));
        reply.setParentComment(new Comment());

        service.notifyCommentReaction(reply, reactor, ReactionType.DISLIKE);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notifications).save(captor.capture());
        assertEquals(NotificationType.REPLY_COMMENT_DISLIKE, captor.getValue().getType());
        assertEquals("Viewer disliked your comment", captor.getValue().getMessage());
    }

    private static AppUser user(Long id, String displayName) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(displayName.toLowerCase());
        user.setDisplayName(displayName);
        return user;
    }

    private static Channel channel(AppUser owner) {
        Channel channel = new Channel();
        channel.setChannelId(3L);
        channel.setOwner(owner);
        channel.setName("Creator channel");
        return channel;
    }

    private static Video video(Channel channel) {
        Video video = new Video();
        video.setVideoId(4L);
        video.setTitle("New video");
        video.setChannel(channel);
        return video;
    }
}
