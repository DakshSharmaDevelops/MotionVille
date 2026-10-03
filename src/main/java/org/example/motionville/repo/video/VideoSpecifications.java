package org.example.motionville.repo.video;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.example.motionville.entity.engagement.VideoView;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class VideoSpecifications {

    private VideoSpecifications() {
    }

    public static Specification<Video> search(
            String search,
            Long categoryId,
            Long channelId,
            boolean publicOnly,
            VideoVisibility publicVisibility,
            Collection<VideoProcessingStatus> playableStatuses) {
        return (root, query, criteriaBuilder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isEmpty()) {
                var channel = root.join("channel", JoinType.INNER);
                String pattern = "%" + search + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern, '!'),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")),
                                pattern,
                                '!'),
                        criteriaBuilder.like(criteriaBuilder.lower(channel.get("name")), pattern, '!'),
                        criteriaBuilder.like(criteriaBuilder.lower(channel.get("handle")), pattern, '!')));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }
            if (channelId != null) {
                predicates.add(criteriaBuilder.equal(root.get("channel").get("channelId"), channelId));
            }
            if (publicOnly) {
                predicates.add(criteriaBuilder.equal(root.get("visibility"), publicVisibility));
                predicates.add(criteriaBuilder.isNotNull(root.get("publishedAt")));
                predicates.add(root.get("processingStatus").in(playableStatuses));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<Video> trending(
            VideoVisibility publicVisibility,
            Collection<VideoProcessingStatus> playableStatuses) {
        return (root, query, criteriaBuilder) -> {
            Subquery<Long> viewCount = query.subquery(Long.class);
            Root<VideoView> view = viewCount.from(VideoView.class);
            viewCount.select(criteriaBuilder.count(view));
            viewCount.where(criteriaBuilder.equal(
                    view.get("video").get("videoId"),
                    root.get("videoId")));

            query.orderBy(
                    criteriaBuilder.desc(viewCount),
                    criteriaBuilder.desc(root.get("publishedAt")),
                    criteriaBuilder.desc(root.get("videoId")));

            return criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("visibility"), publicVisibility),
                    criteriaBuilder.isNotNull(root.get("publishedAt")),
                    root.get("processingStatus").in(playableStatuses));
        };
    }
}
