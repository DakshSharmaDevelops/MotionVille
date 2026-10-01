package org.example.motionville.repo.playlist;

import org.example.motionville.entity.playlist.PlayList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayListRepository extends JpaRepository<PlayList, Long> {
    List<PlayList> findByOwner_Id(Long ownerId);
}
