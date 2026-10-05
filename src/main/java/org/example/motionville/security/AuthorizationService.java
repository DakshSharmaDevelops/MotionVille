package org.example.motionville.security;

import org.example.motionville.entity.playlist.enums.PlayListVisibility;

import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.notification.NotificationRepository;
import org.example.motionville.repo.playlist.PlayListRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("authorizationService")
@Transactional(readOnly = true)
public class AuthorizationService {

    private final AppUserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final VideoRepository videoRepository;
    private final PlayListRepository playListRepository;
    private final CommentRepository commentRepository;
    private final NotificationRepository notificationRepository;

    public AuthorizationService(
            AppUserRepository userRepository,
            ChannelRepository channelRepository,
            VideoRepository videoRepository,
            PlayListRepository playListRepository,
            CommentRepository commentRepository,
            NotificationRepository notificationRepository) {

        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.videoRepository = videoRepository;
        this.playListRepository = playListRepository;
        this.commentRepository = commentRepository;
        this.notificationRepository = notificationRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        return isAuthenticated(authentication)
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    public boolean isEmailVerified(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return false;
        }
        if (isAdmin(authentication)) {
            return true;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(org.example.motionville.entity.account.AppUser::isEmailVerified)
                .orElse(false);
    }

    public boolean isUserOwner(Long userId, Authentication authentication) {
        return matchesUser(userId, authentication);
    }

    public boolean isChannelOwner(
            Long channelId,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return false;
        }

        return channelRepository.findById(channelId)
                .map(channel ->
                        channel.getOwner()
                                .getUsername()
                                .equals(authentication.getName()))
                .orElse(false);
    }

    public boolean isVideoOwner(
            Long videoId,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return false;
        }

        return videoRepository.findById(videoId)
                .map(video ->
                        video.getChannel()
                                .getOwner()
                                .getUsername()
                                .equals(authentication.getName()))
                .orElse(false);
    }

    public boolean isPlaylistOwner(
            Long playlistId,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return false;
        }

        return playListRepository.findById(playlistId)
                .map(playlist ->
                        playlist.getOwner()
                                .getUsername()
                                .equals(authentication.getName()))
                .orElse(false);
    }

    public boolean isCommentOwner(
            Long commentId,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return false;
        }

        return commentRepository.findById(commentId)
                .map(comment ->
                        comment.getAuthor()
                                .getUsername()
                                .equals(authentication.getName()))
                .orElse(false);
    }

    public boolean canDeleteComment(
            Long commentId,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return false;
        }

        return commentRepository.findById(commentId)
                .map(comment -> {
                    String authUser = authentication.getName();
                    if (comment.getAuthor() != null && authUser.equals(comment.getAuthor().getUsername())) {
                        return true;
                    }
                    return comment.getVideo() != null
                            && comment.getVideo().getChannel() != null
                            && comment.getVideo().getChannel().getOwner() != null
                            && authUser.equals(comment.getVideo().getChannel().getOwner().getUsername());
                })
                .orElse(false);
    }

    public boolean isNotificationOwner(
            Long userId,
            Authentication authentication) {

        return matchesUser(userId, authentication);
    }

    public boolean isCurrentUser(
            Long userId,
            Authentication authentication) {

        return matchesUser(userId, authentication);
    }

    public boolean canViewVideo(
            Long videoId,
            Authentication authentication) {

        return videoRepository.findById(videoId)
                .map(video -> {

                    boolean playable =
                            video.getProcessingStatus() == VideoProcessingStatus.READY
                                    || video.getProcessingStatus() == VideoProcessingStatus.UPLOADED;

                    boolean linkAccessible =
                            (video.getVisibility() == VideoVisibility.PUBLIC
                                    || video.getVisibility() == VideoVisibility.UNLISTED)
                                    && video.getPublishedAt() != null
                                    && playable;

                    return linkAccessible
                            || isAdmin(authentication)
                            || isVideoOwner(videoId, authentication);
                })
                .orElse(false);
    }

    public boolean canViewPlaylist(
            Long playlistId,
            Authentication authentication) {

        return playListRepository.findById(playlistId)
                .map(playlist -> {
                    if (playlist.getVisibility() == org.example.motionville.entity.playlist.enums.PlayListVisibility.PUBLIC
                            || playlist.getVisibility() == org.example.motionville.entity.playlist.enums.PlayListVisibility.UNLISTED) {
                        return true;
                    }
                    return isAdmin(authentication) || isPlaylistOwner(playlistId, authentication);
                })
                .orElse(false);
    }

    private boolean matchesUser(
            Long userId,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return false;
        }

        return userRepository.findById(userId)
                .map(user ->
                        user.getUsername()
                                .equals(authentication.getName()))
                .orElse(false);
    }

    public Long getAuthenticatedUserId(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .map(org.example.motionville.entity.account.AppUser::getId)
                .orElse(null);
    }

    private boolean isAuthenticated(Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getName() != null
                && !"anonymousUser".equals(authentication.getName());
    }
}