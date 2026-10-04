package org.example.motionville.repo.playlist;

import org.example.motionville.entity.playlist.PlayList;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayListRepository extends JpaRepository<PlayList, Long> {
    @EntityGraph(attributePaths = {"owner", "videos", "videos.video"})
    List<PlayList> findByOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"owner", "videos", "videos.video"})
    Optional<PlayList> findWithVideosById(Long id);
}
