package org.example.motionville.services.videoService;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.video.Tag;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.video.TagRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideoTagServiceTest {

    private VideoRepository videoRepository;
    private TagRepository tagRepository;
    private VideoTagService service;
    private Video video;
    private Tag tag;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        tagRepository = mock(TagRepository.class);
        service = new VideoTagService(videoRepository, tagRepository);
        video = new Video();
        video.setVideoId(8L);
        video.setChannel(new Channel());
        video.setTags(new ArrayList<>());
        tag = new Tag();
        tag.setId(4L);
        tag.setName("Java");
        when(videoRepository.findById(8L)).thenReturn(Optional.of(video));
        when(tagRepository.findById(4L)).thenReturn(Optional.of(tag));
        when(videoRepository.save(any(Video.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void addsTagToVideo() {
        service.addTag(8L, 4L);

        assertEquals(1, video.getTags().size());
        assertEquals(4L, video.getTags().get(0).getId());
        verify(videoRepository).save(video);
    }

    @Test
    void rejectsDuplicateVideoTag() {
        video.getTags().add(tag);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.addTag(8L, 4L));

        assertEquals(409, exception.getStatusCode().value());
        verify(videoRepository, never()).save(any());
    }

    @Test
    void removesTagFromVideo() {
        video.getTags().add(tag);

        service.removeTag(8L, 4L);

        assertTrue(video.getTags().isEmpty());
        verify(videoRepository).save(video);
    }
}
