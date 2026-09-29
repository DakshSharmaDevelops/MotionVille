package org.example.motionville.engagement;

import java.util.List;
import org.example.motionville.account.UserAccount;
import org.example.motionville.account.UserAccountRepository;
import org.example.motionville.channel.Channel;
import org.example.motionville.channel.ChannelRepository;
import org.example.motionville.channel.ResourceNotFoundException;
import org.example.motionville.video.Video;
import org.example.motionville.video.VideoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EngagementService {
    private final CommentRepository comments;
    private final VideoLikeRepository likes;
    private final SubscriptionRepository subscriptions;
    private final VideoRepository videos;
    private final UserAccountRepository accounts;
    private final ChannelRepository channels;

    public EngagementService(
            CommentRepository comments,
            VideoLikeRepository likes,
            SubscriptionRepository subscriptions,
            VideoRepository videos,
            UserAccountRepository accounts,
            ChannelRepository channels) {
        this.comments = comments;
        this.likes = likes;
        this.subscriptions = subscriptions;
        this.videos = videos;
        this.accounts = accounts;
        this.channels = channels;
    }

    @Transactional(readOnly = true)
    public List<Comment> commentsForVideo(Long videoId) {
        return comments.findByVideoIdAndDeletedFalseOrderByCreatedAtDesc(videoId);
    }

    @Transactional
    public void addComment(Long videoId, Long authorId, String body) {
        String text = body == null ? "" : body.trim();
        if (text.isBlank() || text.length() > 2000) {
            throw new EngagementException("Comments must be between 1 and 2,000 characters.");
        }
        Video video = videos.findById(videoId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found."));
        ensureCanEngage(video, authorId);
        UserAccount author = accounts.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        comments.save(new Comment(video, author, text));
    }

    @Transactional
    public void setLike(Long videoId, Long userId, boolean liked) {
        Video video = videos.findById(videoId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Video not found."));
        ensureCanEngage(video, userId);
        var existing = likes.findByUserIdAndVideoId(userId, videoId);
        if (!liked && existing.isPresent()) {
            likes.delete(existing.get());
        } else if (liked && existing.isEmpty()) {
            UserAccount user = accounts.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
            likes.save(new VideoLike(user, video));
        }
    }

    @Transactional(readOnly = true)
    public long countLikes(Long videoId) {
        return likes.countByVideoId(videoId);
    }

    @Transactional(readOnly = true)
    public boolean isLiked(Long userId, Long videoId) {
        return likes.existsByUserIdAndVideoId(userId, videoId);
    }

    @Transactional
    public void setSubscription(Long channelId, Long userId, boolean subscribed) {
        Channel channel = channels.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found."));
        if (subscribed && channel.getOwner().getId().equals(userId)) {
            throw new EngagementException("You cannot subscribe to your own channel.");
        }
        var existing = subscriptions.findBySubscriberIdAndChannelId(userId, channelId);
        if (!subscribed && existing.isPresent()) {
            subscriptions.delete(existing.get());
        } else if (subscribed && existing.isEmpty()) {
            UserAccount subscriber = accounts.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
            subscriptions.save(new Subscription(subscriber, channel));
        }
    }

    @Transactional(readOnly = true)
    public long countSubscribers(Long channelId) {
        return subscriptions.countByChannelId(channelId);
    }

    @Transactional(readOnly = true)
    public boolean isSubscribed(Long userId, Long channelId) {
        return subscriptions.findBySubscriberIdAndChannelId(userId, channelId).isPresent();
    }

    @Transactional(readOnly = true)
    public List<Subscription> subscriptionsForUser(Long userId) {
        return subscriptions.findBySubscriberIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public Long deleteComment(Long commentId, Long actorId) {
        Comment comment = comments.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found."));
        boolean isAuthor = comment.getAuthor().getId().equals(actorId);
        boolean isChannelOwner = comment.getVideo().getChannel().getOwner().getId().equals(actorId);
        if (!isAuthor && !isChannelOwner) {
            throw new AccessDeniedException("Only the author or channel owner can delete this comment.");
        }
        comment.softDelete();
        return comment.getVideo().getId();
    }

    private void ensureCanEngage(Video video, Long userId) {
        if (video.getVisibility() == org.example.motionville.video.VideoVisibility.PRIVATE
                && !video.getChannel().getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("This video is private.");
        }
    }

    public static class EngagementException extends RuntimeException {
        public EngagementException(String message) {
            super(message);
        }
    }
}
