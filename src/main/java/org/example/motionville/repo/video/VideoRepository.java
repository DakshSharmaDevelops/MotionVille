package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    boolean existsByCategory_Id(Long categoryId);

    boolean existsByTags_Id(Long tagId);

    @EntityGraph(attributePaths = {"channel", "category"})
    java.util.List<Video> findDistinctByTags_Id(Long tagId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.videoId = :id")
    Optional<Video> findForProcessing(@Param("id") Long id);

    @EntityGraph(attributePaths = {"channel", "category"})
    @Query(
            value = """
                    select v from Video v
                    left join v.category c
                    join v.channel ch
                    where (:search = ''
                        or lower(v.title) like lower(concat('%', :search, '%')) escape '!'
                        or lower(coalesce(v.description, '')) like lower(concat('%', :search, '%')) escape '!'
                        or lower(ch.name) like lower(concat('%', :search, '%')) escape '!'
                        or lower(ch.handle) like lower(concat('%', :search, '%')) escape '!')
                    and (:categoryId is null or c.id = :categoryId)
                    and (:channelId is null or v.channel.channelId = :channelId)
                    and (:publicOnly = false or (
                        v.visibility = :publicVisibility
                        and v.publishedAt is not null
                        and v.processingStatus in :playableStatuses
                    ))
                    """,
            countQuery = """
                    select count(v) from Video v
                    left join v.category c
                    join v.channel ch
                    where (:search = ''
                        or lower(v.title) like lower(concat('%', :search, '%')) escape '!'
                        or lower(coalesce(v.description, '')) like lower(concat('%', :search, '%')) escape '!'
                        or lower(ch.name) like lower(concat('%', :search, '%')) escape '!'
                        or lower(ch.handle) like lower(concat('%', :search, '%')) escape '!')
                    and (:categoryId is null or c.id = :categoryId)
                    and (:channelId is null or v.channel.channelId = :channelId)
                    and (:publicOnly = false or (
                        v.visibility = :publicVisibility
                        and v.publishedAt is not null
                        and v.processingStatus in :playableStatuses
                    ))
                    """
    )
    Page<Video> searchVideos(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("channelId") Long channelId,
            @Param("publicOnly") boolean publicOnly,
            @Param("publicVisibility") VideoVisibility publicVisibility,
            @Param("playableStatuses") Collection<VideoProcessingStatus> playableStatuses,
            Pageable pageable);
}
