package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}
