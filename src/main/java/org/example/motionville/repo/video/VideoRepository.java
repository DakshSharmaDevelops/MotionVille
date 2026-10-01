package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.videoId = :id")
    Optional<Video> findForProcessing(@Param("id") Long id);

    @EntityGraph(attributePaths = {"channel", "category"})
    @Query(
            value = """
                    select v from Video v
                    left join v.category c
                    where (:search is null
                        or lower(v.title) like lower(concat('%', :search, '%')) escape '!'
                        or lower(coalesce(v.description, '')) like lower(concat('%', :search, '%')) escape '!')
                    and (:categoryId is null or c.id = :categoryId)
                    and (:channelId is null or v.channel.channelId = :channelId)
                    """,
            countQuery = """
                    select count(v) from Video v
                    left join v.category c
                    where (:search is null
                        or lower(v.title) like lower(concat('%', :search, '%')) escape '!'
                        or lower(coalesce(v.description, '')) like lower(concat('%', :search, '%')) escape '!')
                    and (:categoryId is null or c.id = :categoryId)
                    and (:channelId is null or v.channel.channelId = :channelId)
                    """
    )
    Page<Video> searchVideos(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("channelId") Long channelId,
            Pageable pageable);
}
