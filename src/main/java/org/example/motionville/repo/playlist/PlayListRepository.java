package org.example.motionville.repo.playlist;

import org.example.motionville.entity.playlist.PlayList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayListRepository extends JpaRepository<PlayList, Long> {
}
