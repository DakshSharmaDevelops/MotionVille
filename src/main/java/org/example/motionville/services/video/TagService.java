package org.example.motionville.services.video;

import org.example.motionville.dto.video.TagRequest;
import org.example.motionville.dto.video.TagResponse;
import org.example.motionville.dto.video.VideoResponse;
import org.example.motionville.entity.video.Tag;
import org.example.motionville.entity.video.enums.VideoProcessingStatus;
import org.example.motionville.entity.video.enums.VideoVisibility;
import org.example.motionville.repo.video.TagRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final VideoRepository videoRepository;
    private final R2StorageService r2StorageService;

    @Autowired
    public TagService(
            TagRepository tagRepository,
            VideoRepository videoRepository,
            @Lazy R2StorageService r2StorageService) {
        this.tagRepository = tagRepository;
        this.videoRepository = videoRepository;
        this.r2StorageService = r2StorageService;
    }

    public TagService(
            TagRepository tagRepository,
            VideoRepository videoRepository) {
        this(tagRepository, videoRepository, null);
    }

    @Transactional
    public TagResponse create(TagRequest request) {
        String name = normalizeName(request.name());
        if (tagRepository.existsByNameIgnoreCase(name)) {
            throw conflict("Tag name already exists");
        }
        Tag tag = new Tag();
        tag.setName(name);
        return toResponse(tagRepository.save(tag));
    }

    public List<TagResponse> getAll() {
        return tagRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public TagResponse get(Long id) {
        return toResponse(findTag(id));
    }

    @Transactional
    public TagResponse update(Long id, TagRequest request) {
        Tag tag = findTag(id);
        String name = normalizeName(request.name());
        if (tagRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw conflict("Tag name already exists");
        }
        tag.setName(name);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long id) {
        Tag tag = findTag(id);
        if (videoRepository.existsByTags_Id(id)) {
            throw conflict("Remove this tag from videos before deleting it");
        }
        tagRepository.delete(tag);
    }

    public List<VideoResponse> getVideos(Long tagId) {
        findTag(tagId);
        return videoRepository.findDistinctByTags_Id(tagId).stream()
                .filter(v -> v.getVisibility() == VideoVisibility.PUBLIC
                        && v.getPublishedAt() != null
                        && (v.getProcessingStatus() == VideoProcessingStatus.READY
                            || v.getProcessingStatus() == VideoProcessingStatus.UPLOADED))
                .map(video -> VideoResponseMapper.toResponse(video, r2StorageService))
                .toList();
    }

    private Tag findTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName());
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
