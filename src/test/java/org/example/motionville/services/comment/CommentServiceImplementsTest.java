package org.example.motionville.services.comment;

import org.example.motionville.dto.comment.CommentCreateRequest;
import org.example.motionville.dto.comment.CommentResponse;
import org.example.motionville.services.notification.NotificationCreationService;

import org.example.motionville.entity.comment.Comment;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.comment.CommentRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CommentServiceImplementsTest {

    private CommentRepository commentRepository;
    private AppUserRepository appUserRepository;
    private VideoRepository videoRepository;
    private AuthorizationService authorizationService;
    private NotificationCreationService notificationCreationService;
    private CommentServiceImplements commentService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        videoRepository = mock(VideoRepository.class);
        authorizationService = mock(AuthorizationService.class);
        notificationCreationService = mock(NotificationCreationService.class);
        commentService = new CommentServiceImplements(
                commentRepository,
                videoRepository,
                appUserRepository,
                notificationCreationService,
                authorizationService);
    }

    @Test
    void deletingCommentSoftDeletesItAndPreservesTheRow() {
        Comment comment = new Comment();
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L);

        assertTrue(comment.isDeleted());
        verify(commentRepository).save(comment);
    }

    @Test
    void deletesDeletedCommentDoesNothingAdditional() {
        Comment comment = new Comment();
        comment.setDeleted(true);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        commentService.deleteComment(1L);

        verify(commentRepository, never()).save(comment);
        verify(commentRepository, never()).delete(comment);
    }

    @Test
    void allowsReplyToAReply() {
        Video video = new Video();
        video.setVideoId(4L);
        Comment parentReply = new Comment();
        parentReply.setId(2L);
        parentReply.setVideo(video);
        parentReply.setParentComment(new Comment());
        when(commentRepository.findById(2L)).thenReturn(Optional.of(parentReply));

        AppUser author = new AppUser();
        author.setId(5L);
        author.setDisplayName("Replier");
        when(appUserRepository.findById(5L)).thenReturn(Optional.of(author));

        Comment savedReply = new Comment();
        savedReply.setId(3L);
        savedReply.setVideo(video);
        savedReply.setParentComment(parentReply);
        savedReply.setAuthor(author);
        savedReply.setBody("nested reply");
        savedReply.setCreatedAt(Instant.now());
        savedReply.setUpdatedAt(Instant.now());
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);

        CommentCreateRequest request = new CommentCreateRequest();
        request.setAuthorId(5L);
        request.setBody("nested reply");

        CommentResponse response = commentService.createReply(2L, request);

        assertNotNull(response);
        assertEquals(3L, response.getId());
        assertEquals(2L, response.getParentCommentId());
        assertEquals("nested reply", response.getBody());
        verify(notificationCreationService).notifyCommentReply(any(Comment.class));
    }

    @Test
    void commentReadsReplaceDeletedBodyWithTombstone() {
        Video video = new Video();
        video.setVideoId(4L);
        AppUser author = new AppUser();
        author.setId(7L);
        author.setDisplayName("Author");
        Comment comment = new Comment();
        comment.setVideo(video);
        comment.setAuthor(author);
        comment.setBody("private deleted text");
        comment.setDeleted(true);
        when(videoRepository.existsById(4L)).thenReturn(true);
        when(commentRepository.findByVideo_VideoIdAndParentCommentIsNullOrderByCreatedAtAsc(4L))
                .thenReturn(List.of(comment));

        var result = commentService.findByVideoId(4L).get(0);

        assertTrue(result.isDeleted());
        assertEquals("This comment was deleted", result.getBody());
    }
}
