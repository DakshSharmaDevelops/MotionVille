package org.example.motionville.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.CommentCreateRequest;
import org.example.motionville.dto.CommentRequest;
import org.example.motionville.dto.CommentResponse;
import org.example.motionville.services.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentsController {

    private final CommentService commentService;

    @GetMapping("/videos/{videoId}/comments")
    public List<CommentResponse> getComments(@PathVariable Long videoId) {
        return commentService.findByVideoId(videoId);
    }

    @PostMapping("/videos/{videoId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(@PathVariable Long videoId,
                                         @Valid @RequestBody CommentCreateRequest request) {
        return commentService.createComment(videoId, request);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse updateComment(@PathVariable Long id,
                                         @Valid @RequestBody CommentRequest request) {
        return commentService.updateComment(id, request);
    }

    @DeleteMapping("/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
    }

    @GetMapping("/comments/{commentId}/replies")
    public List<CommentResponse> getReplies(@PathVariable Long commentId) {
        return commentService.findReplies(commentId);
    }

    @PostMapping("/comments/{commentId}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createReply(@PathVariable Long commentId,
                                       @Valid @RequestBody CommentCreateRequest request) {
        return commentService.createReply(commentId, request);
    }
}
