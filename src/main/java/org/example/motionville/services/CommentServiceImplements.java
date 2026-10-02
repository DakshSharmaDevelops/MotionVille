package org.example.motionville.services;

import lombok.RequiredArgsConstructor;
import org.example.motionville.dto.CommentCreateRequest;
import org.example.motionville.dto.CommentRequest;
import org.example.motionville.dto.CommentResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImplements implements CommentService {

    private final CommentRepository commentRepository;
    private final VideoRepository videoRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationCreationService notificationCreationService;

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findByVideoId(Long videoId) {
        if (!videoRepository.existsById(videoId)) {
            throw notFound("Video", videoId);
        }
        return commentRepository.findByVideo_VideoIdAndParentCommentIsNullOrderByCreatedAtAsc(videoId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findReplies(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw notFound("Comment", commentId);
        }
        return commentRepository.findByParentComment_IdOrderByCreatedAtAsc(commentId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public CommentResponse createComment(Long videoId, CommentCreateRequest request) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> notFound("Video", videoId));
        AppUser author = appUserRepository.findById(request.getAuthorId())
                .orElseThrow(() -> notFound("User", request.getAuthorId()));
        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setAuthor(author);
        comment.setBody(request.getBody().trim());
        Instant now = Instant.now();
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        Comment savedComment = commentRepository.save(comment);
        notificationCreationService.notifyNewComment(savedComment);
        return toResponse(savedComment);
    }

    @Override
    public CommentResponse createReply(Long commentId, CommentCreateRequest request) {
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> notFound("Comment", commentId));
        AppUser author = appUserRepository.findById(request.getAuthorId())
                .orElseThrow(() -> notFound("User", request.getAuthorId()));
        Comment reply = new Comment();
        reply.setVideo(parent.getVideo());
        reply.setParentComment(parent);
        reply.setAuthor(author);
        reply.setBody(request.getBody().trim());
        Instant now = Instant.now();
        reply.setCreatedAt(now);
        reply.setUpdatedAt(now);
        Comment savedReply = commentRepository.save(reply);
        notificationCreationService.notifyCommentReply(savedReply);
        return toResponse(savedReply);
    }

    @Override
    public CommentResponse updateComment(Long id, CommentRequest request) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> notFound("Comment", id));
        comment.setBody(request.getBody().trim());
        comment.setUpdatedAt(Instant.now());
        return toResponse(comment);
    }

    @Override
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> notFound("Comment", id));
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setVideoId(comment.getVideo().getVideoId());
        response.setAuthorId(comment.getAuthor().getId());
        response.setAuthorDisplayName(comment.getAuthor().getDisplayName());
        response.setParentCommentId(comment.getParentComment() == null
                ? null : comment.getParentComment().getId());
        response.setBody(comment.getBody());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        return response;
    }

    private ResponseStatusException notFound(String type, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " " + id + " not found");
    }
}
