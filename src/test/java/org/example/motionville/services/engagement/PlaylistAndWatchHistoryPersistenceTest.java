package org.example.motionville.services.engagement;

import org.example.motionville.dto.engagement.WatchHistoryUpdateRequest;
import org.example.motionville.dto.playlist.PlayListCreateRequest;
import org.example.motionville.dto.playlist.PlayListResponse;

import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.playlist.PlayListVideo;
import org.example.motionville.entity.video.Video;

import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;

import org.example.motionville.repo.channel.ChannelRepository;

import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.example.motionville.repo.playlist.PlayListRepository;

import org.example.motionville.repo.playlist.PlayListVideoRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.services.playlist.PlayListService;

import org.example.motionville.services.playlist.PlayListVideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class PlaylistAndWatchHistoryPersistenceTest {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private PlayListRepository playListRepository;

    @Autowired
    private PlayListVideoRepository playListVideoRepository;

    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    @Autowired
    private PlayListService playListService;

    @Autowired
    private PlayListVideoService playListVideoService;

    @Autowired
    private WatchHistoryService watchHistoryService;

    private AppUser owner;
    private Video video1;
    private Video video2;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setEmail("owner@example.test");
        owner.setUsername("owner");
        owner.setPassword("password-hash");
        owner.setDisplayName("Owner");
        owner = appUserRepository.saveAndFlush(owner);

        Channel channel = new Channel();
        channel.setOwner(owner);
        channel.setHandle("@owner");
        channel.setName("Owner channel");
        channel = channelRepository.saveAndFlush(channel);

        video1 = createVideo(channel, "Video 1");
        video2 = createVideo(channel, "Video 2");
    }

    @Test
    void testPlayListManagementAndReordering() {

        PlayListCreateRequest createRequest = new PlayListCreateRequest();
        createRequest.setOwnerId(owner.getId());
        createRequest.setTitle("My Test Playlist");

        PlayListResponse playlist = playListService.savePlayList(createRequest);
        assertNotNull(playlist.getId());

        playListVideoService.addVideo(playlist.getId(), video1.getVideoId());
        playListVideoService.addVideo(playlist.getId(), video2.getVideoId());

        List<PlayListVideo> videos = playListVideoRepository
                .findByPlayList_IdOrderByPositionAsc(playlist.getId());

        assertEquals(2, videos.size());
        assertEquals(0, videos.get(0).getPosition());
        assertEquals(video1.getVideoId(), videos.get(0).getVideo().getVideoId());
        assertEquals(1, videos.get(1).getPosition());
        assertEquals(video2.getVideoId(), videos.get(1).getVideo().getVideoId());

        playListVideoService.reorderVideos(
                playlist.getId(),
                List.of(video2.getVideoId(), video1.getVideoId())
        );

        List<PlayListVideo> reordered = playListVideoRepository
                .findByPlayList_IdOrderByPositionAsc(playlist.getId());

        assertEquals(0, reordered.get(0).getPosition());
        assertEquals(video2.getVideoId(), reordered.get(0).getVideo().getVideoId());
        assertEquals(1, reordered.get(1).getPosition());
        assertEquals(video1.getVideoId(), reordered.get(1).getVideo().getVideoId());
    }

    @Test
    void testWatchHistoryRecordingAndPruning() {

        for (int i = 0; i < 505; i++) {

            Video video = createVideo(video1.getChannel(), "Bulk Video " + i);

            WatchHistoryUpdateRequest req = new WatchHistoryUpdateRequest();
            req.setUserId(owner.getId());
            req.setLastPositionSeconds(10);
            watchHistoryService.recordProgress(video.getVideoId(), req);
        }

        long count = watchHistoryRepository.findByUser_IdOrderByLastWatchedAtDesc(owner.getId()).size();

        assertEquals(500, count);
    }

    private Video createVideo(Channel channel, String title) {

        Video video = new Video();
        video.setChannel(channel);
        video.setTitle(title);
        video.setDurationSeconds(120);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setCreatedAt(Instant.now());
        video.setUpdatedAt(Instant.now());

        return videoRepository.saveAndFlush(video);
    }
}
