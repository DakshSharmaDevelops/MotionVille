package org.example.motionville.services.comment;

import org.example.motionville.services.notification.NotificationCreationService;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.comment.CommentReactionResponse;
import org.example.motionville.dto.comment.CommentReactionSummary;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.comment.CommentReaction;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentReactionRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.security.AuthorizationService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentReactionServiceImplements implements CommentReactionService{

    private final CommentReactionRepository commentReactionRepository;

    private final CommentRepository commentRepository;

    private final AppUserRepository appUserRepository;
    private final NotificationCreationService notificationCreationService;
    private final AuthorizationService authorizationService;


    @Override
    @Transactional(readOnly = true)
    public CommentReactionSummary getCommentReactionSummary(Long commentId, Long userId) {
        Comment comment = requireComment(commentId);
        requireCommentVideoAccess(comment);
        ReactionType reactionType =userId== null ? null :
                commentReactionRepository.findByComment_IdAndUser_Id(commentId,userId)
                        .map(CommentReaction::getReaction)
                        .orElse(null);


        return new CommentReactionSummary(
                commentReactionRepository.countByComment_IdAndReaction(commentId,ReactionType.LIKE),
                commentReactionRepository.countByComment_IdAndReaction(commentId,ReactionType.DISLIKE),
                reactionType);
    }

    @Transactional
    @Override
    public CommentReactionResponse setCommentReaction(Long commentId, Long userId, ReactionType reactionType){
        Comment comment=requireComment(commentId);
        requireCommentVideoAccess(comment);

        AppUser user=appUserRepository.findById(userId)
                .orElseThrow(()->
                        notFound("user",userId));

        CommentReaction reaction= commentReactionRepository.
                findByComment_IdAndUser_Id(commentId,userId)
                .orElseGet(()->
                        CommentReaction.builder()
                                .comment(comment)
                                .user(user)
                                .build());

        ReactionType previousReaction = reaction.getReaction();
        reaction.setComment(comment);
        reaction.setUser(user);
        reaction.setReaction(reactionType);
        CommentReaction newReaction= commentReactionRepository.save(reaction);
        if (previousReaction != reactionType) {
            notificationCreationService.notifyCommentReaction(comment, user, reactionType);
        }
        return new CommentReactionResponse(commentId,userId,newReaction.getReaction());
    }

    @Transactional
    @Override
    public void deleteCommentReaction(Long commentId, Long userId){
        Comment comment = requireComment(commentId);
        requireCommentVideoAccess(comment);

        commentReactionRepository
                .findByComment_IdAndUser_Id(commentId, userId)
                .ifPresent(commentReactionRepository::delete);
    }

    private Comment requireComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> notFound("comment",commentId));
        if (comment.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Comment " + commentId + " was deleted");
        }
        return comment;
    }

    private void requireCommentVideoAccess(Comment comment) {
        if (comment.getVideo() == null) {
            throw notFound("comment", comment.getId());
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long videoId = comment.getVideo().getVideoId();
        if (!authorizationService.canViewVideo(videoId, authentication)) {
            throw notFound("comment", comment.getId());
        }
    }

    private ResponseStatusException notFound(String resource,Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,
                resource+ " " + id + " not found");
    }
}
