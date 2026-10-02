package org.example.motionville.services;

import org.example.motionville.dto.PlayListVideoResponse;
import org.example.motionville.dto.WatchHistoryResponse;
import org.example.motionville.dto.WatchHistoryUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.playlist.PlayList;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.engagement.WatchHistoryRepository;
import org.example.motionville.repo.playlist.PlayListRepository;
import org.example.motionville.repo.playlist.PlayListVideoRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@DataJpaTest
@ActiveProfiles("test")
@Import({PlayListVideoServiceImplements.class, WatchHistoryServiceImplements.class})
class PlaylistAndWatchHistoryPersistenceTest {

    @Autowired private AppUserRepository appUserRepository;
    @Autowired private ChannelRepository channelRepository;
    @Autowired private VideoRepository videoRepository;
    @Autowired private PlayListRepository playListRepository;
    @Autowired private PlayListVideoRepository playListVideoRepository;
    @Autowired private WatchHistoryRepository watchHistoryRepository;
    @Autowired private PlayListVideoServiceImplements playListVideoService;
    @Autowired private WatchHistoryServiceImplements watchHistoryService;

    private AppUser owner;
    private Video firstVideo;
    private Video secondVideo;
    private PlayList playList;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setEmail("owner@example.test");
        owner.setUsername("owner");
        owner.setPassword("password");
        owner.setPasswordHash("password-hash");
        owner.setDisplayName("Owner");
        owner = appUserRepository.saveAndFlush(owner);

        Channel channel = new Channel();
        channel.setOwner(owner);
        channel.setHandle("@owner");
        channel.setName("Owner channel");
        channel = channelRepository.saveAndFlush(channel);

        firstVideo = saveVideo(channel, "First");
        secondVideo = saveVideo(channel, "Second");

        playList = new PlayList();
        playList.setOwner(owner);
        playList.setTitle("Favorites");
        playList = playListRepository.saveAndFlush(playList);
    }

    @Test
    void persistsPlaylistOrderAndNormalizesPositionsAfterRemoval() {
        playListVideoService.addVideo(playList.getId(), firstVideo.getVideoId());
        playListVideoService.addVideo(playList.getId(), secondVideo.getVideoId());

        List<PlayListVideoResponse> reordered = playListVideoService.reorderVideos(
                playList.getId(),
                List.of(secondVideo.getVideoId(), firstVideo.getVideoId()));

        assertEquals(List.of(secondVideo.getVideoId(), firstVideo.getVideoId()),
                reordered.stream().map(PlayListVideoResponse::getVideoId).toList());
        assertEquals(List.of(0, 1), reordered.stream().map(PlayListVideoResponse::getPosition).toList());

        playListVideoService.removeVideo(playList.getId(), secondVideo.getVideoId());

        List<PlayListVideoResponse> remaining = playListVideoService.listVideos(playList.getId());
        assertEquals(1, remaining.size());
        assertEquals(firstVideo.getVideoId(), remaining.get(0).getVideoId());
        assertEquals(0, remaining.get(0).getPosition());
        assertEquals(1, playListVideoRepository.count());
    }

    @Test
    void updatesExistingWatchHistoryRowWhenProgressIsRecordedAgain() {
        WatchHistoryUpdateRequest firstProgress = progress(12);
        WatchHistoryResponse first = watchHistoryService.recordProgress(
                firstVideo.getVideoId(), firstProgress);

        WatchHistoryResponse updated = watchHistoryService.recordProgress(
                firstVideo.getVideoId(), progress(28));
        List<WatchHistoryResponse> history = watchHistoryService.getHistory(owner.getId());

        assertEquals(first.getId(), updated.getId());
        assertNotEquals(first.getLastPositionSeconds(), updated.getLastPositionSeconds());
        assertEquals(28, updated.getLastPositionSeconds());
        assertEquals(1, history.size());
        assertEquals(1, watchHistoryRepository.count());
    }

    private Video saveVideo(Channel channel, String title) {
        Video video = new Video();
        video.setChannel(channel);
        video.setTitle(title);
        video.setDurationSeconds(120);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        return videoRepository.saveAndFlush(video);
    }

    private WatchHistoryUpdateRequest progress(int seconds) {
        WatchHistoryUpdateRequest request = new WatchHistoryUpdateRequest();
        request.setUserId(owner.getId());
        request.setLastPositionSeconds(seconds);
        return request;
    }
}
