package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationCreationService {

    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;

    public void notifyNewVideo(Video video) {
        AppUser actor = video.getChannel().getOwner();
        List<Notification> notifications = new ArrayList<>();
        for (Subscription subscription :
                subscriptionRepository.findByChannel_ChannelId(video.getChannel().getChannelId())) {
            AppUser recipient = subscription.getSubscriber();
            if (recipient.getId().equals(actor.getId())) continue;
            notifications.add(build(
                    recipient,
                    actor,
                    video,
                    null,
                    NotificationType.NEW_VIDEO,
                    displayName(actor) + " uploaded a new video: " + video.getTitle()));
        }
        notificationRepository.saveAll(notifications);
    }

    public void notifyNewSubscriber(Channel channel, AppUser actor) {
        AppUser recipient = channel.getOwner();
        if (recipient.getId().equals(actor.getId())) return;
        notificationRepository.save(build(
                recipient,
                actor,
                null,
                null,
                NotificationType.NEW_SUBSCRIBER,
                displayName(actor) + " subscribed to your channel"));
    }

    public void notifyNewComment(Comment comment) {
        AppUser recipient = comment.getVideo().getChannel().getOwner();
        AppUser actor = comment.getAuthor();
        if (recipient.getId().equals(actor.getId())) return;
        notificationRepository.save(build(
                recipient,
                actor,
                comment.getVideo(),
                comment,
                NotificationType.NEW_COMMENT,
                displayName(actor) + " commented on your video: " + comment.getVideo().getTitle()));
    }

    public void notifyCommentReply(Comment reply) {
        AppUser recipient = reply.getParentComment().getAuthor();
        AppUser actor = reply.getAuthor();
        if (recipient.getId().equals(actor.getId())) return;
        notificationRepository.save(build(
                recipient,
                actor,
                reply.getVideo(),
                reply,
                NotificationType.COMMENT_REPLY,
                displayName(actor) + " replied to your comment"));
    }

    public void notifyVideoReaction(Video video, AppUser actor, ReactionType reactionType) {
        AppUser recipient = video.getChannel().getOwner();
        if (recipient.getId().equals(actor.getId())) return;
        boolean liked = reactionType == ReactionType.LIKE;
        notificationRepository.save(build(
                recipient,
                actor,
                video,
                null,
                liked ? NotificationType.VIDEO_LIKE : NotificationType.VIDEO_DISLIKE,
                displayName(actor) + (liked ? " liked your video: " : " disliked your video: ")
                        + video.getTitle()));
    }

    public void notifyCommentReaction(Comment comment, AppUser actor, ReactionType reactionType) {
        AppUser recipient = comment.getAuthor();
        if (recipient.getId().equals(actor.getId())) return;
        boolean liked = reactionType == ReactionType.LIKE;
        NotificationType type;
        if (comment.getParentComment() == null) {
            type = liked ? NotificationType.COMMENT_LIKE : NotificationType.COMMENT_DISLIKE;
        } else {
            type = liked
                    ? NotificationType.REPLY_COMMENT_LIKE
                    : NotificationType.REPLY_COMMENT_DISLIKE;
        }
        notificationRepository.save(build(
                recipient,
                actor,
                comment.getVideo(),
                comment,
                type,
                displayName(actor) + (liked ? " liked your comment" : " disliked your comment")));
    }

    private Notification build(
            AppUser recipient,
            AppUser actor,
            Video video,
            Comment comment,
            NotificationType type,
            String message) {
        return Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .video(video)
                .comment(comment)
                .type(type)
                .message(message.length() <= 255 ? message : message.substring(0, 255))
                .build();
    }

    private String displayName(AppUser user) {
        return user.getDisplayName() == null || user.getDisplayName().isBlank()
                ? user.getUsername()
                : user.getDisplayName();
    }
}
