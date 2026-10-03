package org.example.motionville.services.comment;

import org.example.motionville.dto.comment.CommentCreateRequest;
import org.example.motionville.dto.comment.CommentRequest;
import org.example.motionville.dto.comment.CommentResponse;

import java.util.List;

public interface CommentService {
    List<CommentResponse> findByVideoId(Long videoId);
    List<CommentResponse> findReplies(Long commentId);
    CommentResponse createComment(Long videoId, CommentCreateRequest request);
    CommentResponse createReply(Long commentId, CommentCreateRequest request);
    CommentResponse updateComment(Long id, CommentRequest request);
    void deleteComment(Long id);
}
