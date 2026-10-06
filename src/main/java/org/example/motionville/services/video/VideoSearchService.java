package org.example.motionville.services.video;

import org.example.motionville.dto.video.VideoPageResponse;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.video.VideoRepository;
import org.example.motionville.repo.video.VideoSpecifications;
import org.example.motionville.security.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class VideoSearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "updatedAt", "publishedAt", "title", "durationSeconds", "id");

    private final VideoRepository videoRepository;
    private final AuthorizationService authorizationService;
    private final R2StorageService r2StorageService;

    @Autowired
    public VideoSearchService(
            VideoRepository videoRepository,
            AuthorizationService authorizationService,
            @Lazy R2StorageService r2StorageService) {
        this.videoRepository = videoRepository;
        this.authorizationService = authorizationService;
        this.r2StorageService = r2StorageService;
    }

    public VideoSearchService(
            VideoRepository videoRepository,
            AuthorizationService authorizationService) {
        this(videoRepository, authorizationService, null);
    }

    public VideoPageResponse search(
            String search,
            int page,
            int size,
            String sort,
            Long categoryId,
            Long channelId,
            boolean publicOnly) {
        validateFilters(page, size, categoryId, channelId);
        publicOnly = enforceVisibility(publicOnly, channelId);
        Sort parsedSort = parseSort(sort);
        String normalizedSearch = normalizeSearch(search);
        Pageable pageable = PageRequest.of(page, size, stableSort(parsedSort));
        Page<Video> result = videoRepository.findAll(VideoSpecifications.search(
                normalizedSearch,
                categoryId,
                channelId,
                publicOnly,
                VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                pageable);

        return new VideoPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(video -> VideoResponseMapper.toResponse(video, r2StorageService)).toList()
        );
    }

    public VideoPageResponse getTrending(int page, int size) {
        int boundedPage = Math.max(page, 0);
        int boundedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(boundedPage, boundedSize);
        Page<Video> result = videoRepository.findAll(VideoSpecifications.trending(
                VideoVisibility.PUBLIC,
                List.of(VideoProcessingStatus.READY, VideoProcessingStatus.UPLOADED)),
                pageable);

        return new VideoPageResponse(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getContent().stream().map(video -> VideoResponseMapper.toResponse(video, r2StorageService)).toList()
        );
    }


    private boolean enforceVisibility(boolean requestedPublicOnly, Long channelId) {
        if (requestedPublicOnly) return true;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authorizationService.isAdmin(authentication)) return false;

        if (channelId != null && authorizationService.isChannelOwner(channelId, authentication)) {
            return false;
        }

        return true;
    }

    private void validateFilters(int page, int size, Long categoryId, Long channelId) {
        if (page < 0) {
            throw badRequest("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (categoryId != null && categoryId < 1) {
            throw badRequest("categoryId must be greater than zero");
        }
        if (channelId != null && channelId < 1) {
            throw badRequest("channelId must be greater than zero");
        }
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            sort = "createdAt,desc";
        }
        String[] parts = sort.trim().split(",", -1);
        if (parts.length != 2) {
            throw badRequest("sort must use the format field,direction");
        }

        String field = parts[0].trim();
        if (!SORTABLE_FIELDS.contains(field)) {
            throw badRequest("Unsupported sort field: " + field);
        }
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException exception) {
            throw badRequest("Sort direction must be asc or desc");
        }
        String entityField = "id".equals(field) ? "videoId" : field;
        return Sort.by(direction, entityField);
    }

    private Sort stableSort(Sort sort) {
        return sort.getOrderFor("videoId") == null
                ? sort.and(Sort.by(Sort.Direction.DESC, "videoId"))
                : sort;
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return search.trim()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
                .toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
