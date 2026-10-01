package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.CommentReactionResponse;
import org.example.motionville.dto.CommentReactionSummary;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.comment.CommentReaction;
import org.example.motionville.entity.engagement.enums.ReactionType;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentReactionRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentReactionServiceImplements implements CommentReactionService{

    private final CommentReactionRepository commentReactionRepository;

    private final CommentRepository commentRepository;

    private final AppUserRepository appUserRepository;


    @Override
    @Transactional(readOnly = true)
    public CommentReactionSummary getCommentReactionSummary(Long commentId, Long userId) {
        requireComment(commentId);
        ReactionType reactionType =userId== null ? null :
                commentReactionRepository.findByComment_CommentIdAndUserId(commentId,userId)
                        .map(CommentReaction::getReaction)
                        .orElse(null);


        return new CommentReactionSummary(
                commentReactionRepository.countByComment_CommentIdAndReaction(commentId,ReactionType.LIKE),
                commentReactionRepository.countByComment_CommentIdAndReaction(commentId,ReactionType.DISLIKE),
                reactionType);
    }

    @Transactional
    @Override
    public CommentReactionResponse setCommentReaction(Long commentId, Long userId, ReactionType reactionType){
        Comment comment=requireComment(commentId);

        AppUser user=appUserRepository.findById(userId)
                .orElseThrow(()->
                        notFound(userId));

        CommentReaction reaction= commentReactionRepository.
                findByComment_CommentIdAndUserId(commentId,userId)
                .orElseGet(()->
                        CommentReaction.builder()
                                .comment(comment)
                                .user(user)
                                .build());

        reaction.setComment(comment);
        reaction.setUser(user);
        reaction.setReaction(reactionType);
        CommentReaction newReaction= commentReactionRepository.save(reaction);
        return new CommentReactionResponse(commentId,userId,newReaction.getReaction());
    }

    @Transactional
    @Override
    public void deleteCommentReaction(Long commentId, Long userId){
        requireComment(commentId);
        CommentReaction reaction=commentReactionRepository
                    .findByComment_CommentIdAndUserId(commentId,userId)
                    .orElseThrow(()->
                                notFound(userId));
        commentReactionRepository.delete(reaction);
    }

    private Comment requireComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> notFound(commentId));
    }
    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment" + " " + id + " not found");
    }
}
