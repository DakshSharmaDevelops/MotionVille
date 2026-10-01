package org.example.motionville.services.videoService;

import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VideoSearchServiceTest {

    private VideoRepository videoRepository;
    private VideoSearchService service;

    @BeforeEach
    void setUp() {
        videoRepository = mock(VideoRepository.class);
        service = new VideoSearchService(videoRepository);
    }

    @Test
    void appliesPaginationFiltersAndStableSort() {
        Video video = video(42L, 8L, "Java basics");
        when(videoRepository.searchVideos(
                eq("java"), eq(3L), eq(8L), eq(true), eq(VideoVisibility.PUBLIC),
                eq(List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                any(PageRequest.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(video),
                        invocation.getArgument(6),
                        41));

        var response = service.search(" JAVA ", 1, 20, "createdAt,desc", 3L, 8L, true);

        assertEquals(1, response.page());
        assertEquals(20, response.size());
        assertEquals(41, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(42L, response.content().get(0).id());

        var pageable = org.mockito.ArgumentCaptor.forClass(PageRequest.class);
        verify(videoRepository).searchVideos(
                eq("java"),
                eq(3L),
                eq(8L),
                eq(true),
                eq(VideoVisibility.PUBLIC),
                eq(List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(Sort.Direction.DESC,
                pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
        assertEquals(Sort.Direction.DESC,
                pageable.getValue().getSort().getOrderFor("videoId").getDirection());
    }

    @Test
    void escapesLikeWildcardsAndUsesDefaultSort() {
        when(videoRepository.searchVideos(
                eq("100!%!_!!"), isNull(), isNull(), eq(false), eq(VideoVisibility.PUBLIC),
                eq(List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.search(" 100%_! ", 0, 20, null, null, null, false);

        var pageable = org.mockito.ArgumentCaptor.forClass(PageRequest.class);
        verify(videoRepository).searchVideos(
                eq("100!%!_!!"),
                isNull(),
                isNull(),
                eq(false),
                eq(VideoVisibility.PUBLIC),
                eq(List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                pageable.capture());
        assertEquals(Sort.Direction.DESC,
                pageable.getValue().getSort().getOrderFor("createdAt").getDirection());
    }

    @Test
    void usesEmptySearchStringForUnfilteredVideoQuery() {
        when(videoRepository.searchVideos(
                eq(""), isNull(), isNull(), eq(true), eq(VideoVisibility.PUBLIC),
                eq(List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.search("   ", 0, 20, null, null, null, true);

        verify(videoRepository).searchVideos(
                eq(""), isNull(), isNull(), eq(true), eq(VideoVisibility.PUBLIC),
                eq(List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                any(PageRequest.class));
    }

    @Test
    void rejectsInvalidPagingAndUnsupportedSortWithoutQueryingRepository() {
        assertThrows(ResponseStatusException.class,
                () -> service.search(null, -1, 20, "createdAt,desc", null, null, false));
        assertThrows(ResponseStatusException.class,
                () -> service.search(null, 0, 101, "createdAt,desc", null, null, false));
        assertThrows(ResponseStatusException.class,
                () -> service.search(null, 0, 20, "channel.name,desc", null, null, false));
        assertThrows(ResponseStatusException.class,
                () -> service.search(null, 0, 20, "createdAt,sideways", null, null, false));

        verifyNoInteractions(videoRepository);
    }

    private Video video(Long id, Long channelId, String title) {
        Channel channel = new Channel();
        channel.setChannelId(channelId);
        Video video = new Video();
        video.setVideoId(id);
        video.setChannel(channel);
        video.setTitle(title);
        video.setDurationSeconds(120);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        video.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        return video;
    }
}
