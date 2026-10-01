package org.example.motionville.services.videoService;

import org.example.motionville.dto.TagRequest;
import org.example.motionville.entity.video.Tag;
import org.example.motionville.repo.video.TagRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TagServiceTest {

    private TagRepository tagRepository;
    private VideoRepository videoRepository;
    private TagService service;
    private Tag tag;

    @BeforeEach
    void setUp() {
        tagRepository = mock(TagRepository.class);
        videoRepository = mock(VideoRepository.class);
        service = new TagService(tagRepository, videoRepository);
        tag = new Tag();
        tag.setId(5L);
        tag.setName("Java");
        when(tagRepository.findById(5L)).thenReturn(Optional.of(tag));
        when(tagRepository.save(any(Tag.class))).thenAnswer(call -> {
            Tag saved = call.getArgument(0);
            if (saved.getId() == null) saved.setId(5L);
            return saved;
        });
    }

    @Test
    void createsTrimmedTag() {
        when(tagRepository.existsByNameIgnoreCase("Java")).thenReturn(false);

        var response = service.create(new TagRequest(" Java "));

        assertEquals(5L, response.id());
        assertEquals("Java", response.name());
    }

    @Test
    void rejectsDuplicateNameIgnoringCase() {
        when(tagRepository.existsByNameIgnoreCase("java")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(new TagRequest("java")));

        assertEquals(409, exception.getStatusCode().value());
        verify(tagRepository, never()).save(any());
    }

    @Test
    void rejectsDeleteWhenTagIsInUse() {
        when(videoRepository.existsByTags_Id(5L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.delete(5L));

        assertEquals(409, exception.getStatusCode().value());
        verify(tagRepository, never()).delete(tag);
    }
}
