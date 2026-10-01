package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.comment.CommentReaction;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentReactionRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CommentReactionServiceImplements implements CommentReactionService{

    private final CommentReactionRepository commentReactionRepository;

    private final CommentRepository commentRepository;

    private final AppUserRepository appUserRepository;


    @Override
    public CommentReaction findCommentReactionSummary(Long commentId, Long userId) {
        requireComment(commentId);

        return null;
    }

    private Comment requireComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> notFound(commentId));
    }
    private ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment" + " " + id + " not found");
    }
}
