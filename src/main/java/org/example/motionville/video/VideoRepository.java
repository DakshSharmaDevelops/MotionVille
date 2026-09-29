package org.example.motionville.video;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    @Query("""
            select v from Video v
            where v.deleted = false and v.visibility = org.example.motionville.video.VideoVisibility.PUBLIC
              and (:category = '' or lower(coalesce(v.category, '')) = lower(:category))
              and (:query = '' or lower(v.title) like lower(concat('%', :query, '%'))
                   or lower(coalesce(v.description, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(v.tags, '')) like lower(concat('%', :query, '%')))
            order by v.createdAt desc
            """)
    Page<Video> searchPublic(
            @Param("query") String query,
            @Param("category") String category,
            Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    Page<Video> findByChannelIdAndDeletedFalseAndVisibilityOrderByCreatedAtDesc(
            Long channelId, VideoVisibility visibility, Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    Page<Video> findByChannelIdInAndDeletedFalseAndVisibilityOrderByCreatedAtDesc(
            java.util.Collection<Long> channelIds, VideoVisibility visibility, Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    @Query("""
            select v from Video v
            where v.deleted = false and v.visibility = org.example.motionville.video.VideoVisibility.PUBLIC
            order by v.viewCount desc, v.createdAt desc
            """)
    Page<Video> findPublicTrending(Pageable pageable);

    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    Optional<Video> findByStorageKeyAndDeletedFalse(String storageKey);

    @Override
    @EntityGraph(attributePaths = {"channel", "channel.owner"})
    Optional<Video> findById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Video v set v.viewCount = v.viewCount + 1 where v.id = :videoId")
    int incrementViewCount(@Param("videoId") Long videoId);
}
