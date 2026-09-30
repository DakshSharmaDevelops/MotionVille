package org.example.motionville.services;

import org.example.motionville.dto.CommentCreateRequest;
import org.example.motionville.dto.CommentRequest;
import org.example.motionville.dto.CommentResponse;

import java.util.List;

public interface CommentService {
    List<CommentResponse> findByVideoId(Long videoId);
    List<CommentResponse> findReplies(Long commentId);
    CommentResponse createComment(Long videoId, CommentCreateRequest request);
    CommentResponse createReply(Long commentId, CommentCreateRequest request);
    CommentResponse updateComment(Long id, CommentRequest request);
    void deleteComment(Long id);
}
