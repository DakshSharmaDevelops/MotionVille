package org.example.motionville.repo.video;

import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.video.Category;
import org.example.motionville.entity.video.Tag;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class VideoRepositorySearchTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VideoRepository videoRepository;

    private Long channelId;
    private Long categoryId;

    @BeforeEach
    void createSearchFixtures() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        AppUser owner = new AppUser();
        owner.setEmail("video-search@example.test");
        owner.setUsername("video-search");
        owner.setPassword("password");
        owner.setPasswordHash("password-hash");
        owner.setDisplayName("Video Search");
        owner.setCreatedAt(now);
        owner.setUpdatedAt(now);
        entityManager.persist(owner);

        Channel channel = new Channel();
        channel.setOwner(owner);
        channel.setHandle("@video-search");
        channel.setName("Video Search Channel");
        channel.setCreatedAt(now);
        entityManager.persist(channel);

        Category category = new Category();
        category.setName("Technology");
        category.setSlug("technology");
        category.setDescription("Technology videos");
        entityManager.persist(category);

        entityManager.persist(video(channel, category, "Java Basics", "Learn Java", now));
        entityManager.persist(video(channel, null, "Java Advanced", "More lessons", now.plusSeconds(1)));
        entityManager.persist(video(channel, category, "Cooking", "Java-free recipe", now.plusSeconds(2)));
        entityManager.flush();
        entityManager.clear();

        channelId = channel.getChannelId();
        categoryId = category.getId();
    }

    @Test
    void filtersSearchAndCategoryWhileReturningAccuratePages() {
        var allJava = videoRepository.findAll(VideoSpecifications.search(
                "java", null, channelId, false, VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertEquals(3, allJava.getTotalElements());
        assertEquals(3, allJava.getTotalPages());
        assertEquals("Cooking", allJava.getContent().get(0).getTitle());

        var categorizedJava = videoRepository.findAll(VideoSpecifications.search(
                "java", categoryId, channelId, false, VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                PageRequest.of(0, 10));
        assertEquals(2, categorizedJava.getTotalElements());
        assertTrue(categorizedJava.getContent().stream()
                .map(Video::getTitle)
                .toList()
                .containsAll(java.util.Set.of("Java Basics", "Cooking")));
    }

    @Test
    void includesUncategorizedVideosWithoutCategoryFilterAndEscapesLikeWildcards() {
        var unfiltered = videoRepository.findAll(VideoSpecifications.search(
                "", null, channelId, false, VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                PageRequest.of(0, 10));
        assertEquals(3, unfiltered.getTotalElements());

        Video percentTitle = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "100% Java",
                null,
                Instant.now());
        entityManager.persist(percentTitle);
        entityManager.flush();

        var literalPercent = videoRepository.findAll(VideoSpecifications.search(
                "100!%", null, channelId, false, VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                PageRequest.of(0, 10));
        assertEquals(1, literalPercent.getTotalElements());
        assertEquals("100% Java", literalPercent.getContent().get(0).getTitle());
    }

    @Test
    void filtersUnpublishedPrivateAndUnreadyVideosBeforePagination() {
        Instant publishedAt = Instant.parse("2026-02-01T00:00:00Z");

        Video visible = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "Visible public video",
                null,
                publishedAt);
        visible.setPublishedAt(publishedAt);
        entityManager.persist(visible);

        Video unpublished = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "Unpublished video",
                null,
                publishedAt.plusSeconds(1));
        unpublished.setPublishedAt(null);
        entityManager.persist(unpublished);

        Video privateVideo = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "Private video",
                null,
                publishedAt.plusSeconds(2));
        privateVideo.setVisibility(VideoVisibility.PRIVATE);
        privateVideo.setPublishedAt(publishedAt);
        entityManager.persist(privateVideo);

        Video processing = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "Processing video",
                null,
                publishedAt.plusSeconds(3));
        processing.setPublishedAt(publishedAt);
        processing.setProcessingStatus(VideoProcessingStatus.PROCESSING);
        entityManager.persist(processing);
        entityManager.flush();
        entityManager.clear();

        var firstPage = videoRepository.findAll(VideoSpecifications.search(
                "",
                null,
                channelId,
                true,
                VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(1, firstPage.getTotalElements());
        assertEquals(1, firstPage.getTotalPages());
        assertEquals("Visible public video", firstPage.getContent().get(0).getTitle());
    }

    @Test
    void findsVideosByTagThroughManyToManyJoin() {
        Tag tag = new Tag();
        tag.setName("java");
        entityManager.persist(tag);

        Video video = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "Tagged video",
                null,
                Instant.now());
        video.setTags(List.of(tag));
        entityManager.persist(video);
        entityManager.flush();
        entityManager.clear();

        var taggedVideos = videoRepository.findDistinctByTags_Id(tag.getId());

        assertEquals(1, taggedVideos.size());
        assertEquals("Tagged video", taggedVideos.get(0).getTitle());
    }

    @Test
    void enforcesUniqueAssetVariantPerVideo() {
        Video video = video(
                entityManager.getReference(Channel.class, channelId),
                null,
                "Asset test",
                null,
                Instant.now());
        entityManager.persist(video);
        entityManager.flush();

        entityManager.persist(asset(video, "r2://bucket/first"));
        entityManager.flush();
        assertThrows(ConstraintViolationException.class, () -> {
            entityManager.persist(asset(video, "r2://bucket/second"));
            entityManager.flush();
        });
    }

    private VideoAsset asset(Video video, String url) {
        return VideoAsset.builder()
                .video(video)
                .assetUrl(url)
                .quality("playback")
                .mimeType("video/mp4")
                .sizeBytes(10L)
                .createdAt(Instant.now())
                .build();
    }

    private Video video(Channel channel, Category category, String title, String description, Instant createdAt) {
        Video video = new Video();
        video.setChannel(channel);
        video.setCategory(category);
        video.setTitle(title);
        video.setDescription(description);
        video.setDurationSeconds(60);
        video.setVisibility(VideoVisibility.PUBLIC);
        video.setProcessingStatus(VideoProcessingStatus.READY);
        video.setCreatedAt(createdAt);
        video.setUpdatedAt(createdAt);
        return video;
    }
}
