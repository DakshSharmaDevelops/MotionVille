package org.example.motionville.controllers.comment;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.comment.CommentCreateRequest;
import org.example.motionville.dto.comment.CommentRequest;
import org.example.motionville.dto.comment.CommentResponse;
import org.example.motionville.services.comment.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class CommentsController {

    private final CommentService commentService;

    @PreAuthorize("@authorizationService.canViewVideo(#videoId, authentication)")
    @GetMapping("/videos/{videoId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long videoId) {
        return commentService.findByVideoId(videoId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#request.authorId, authentication)")
    @PostMapping("/videos/{videoId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long videoId,
                                         @Valid
                                         @RequestBody CommentCreateRequest request) {
        return commentService.createComment(videoId, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isCommentOwner(#id, authentication)")
    @PutMapping("/comments/{id}")
    public CommentResponse updateComment(@PathVariable Long id,
                                         @Valid @RequestBody CommentRequest request) {
        return commentService.updateComment(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canDeleteComment(#id, authentication)")
    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }

    @GetMapping("/comments/{commentId}/replies")
    public List<CommentResponse> getReplies(@PathVariable Long commentId) {
        return commentService.findReplies(commentId);
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#request.authorId, authentication)")
    @PostMapping("/comments/{commentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createReply(@PathVariable Long commentId,
                                       @Valid @RequestBody CommentCreateRequest request) {
        return commentService.createReply(commentId, request);
    }
}