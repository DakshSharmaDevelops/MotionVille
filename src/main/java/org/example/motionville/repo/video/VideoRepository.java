package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Video;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long>, JpaSpecificationExecutor<Video> {
    boolean existsByCategory_Id(Long categoryId);

    boolean existsByTags_Id(Long tagId);

    @EntityGraph(attributePaths = {"channel", "category"})
    java.util.List<Video> findDistinctByTags_Id(Long tagId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Video> findByVideoId(Long videoId);

    @EntityGraph(attributePaths = {"channel", "category"})
    @Override
    Page<Video> findAll(Specification<Video> specification, Pageable pageable);
}
