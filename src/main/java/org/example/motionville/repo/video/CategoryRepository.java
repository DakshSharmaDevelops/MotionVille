package org.example.motionville.repo.video;

import org.example.motionville.entity.video.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
