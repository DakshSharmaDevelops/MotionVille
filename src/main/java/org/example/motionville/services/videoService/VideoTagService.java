package org.example.motionville.services.videoService;

import org.example.motionville.dto.TagResponse;
import org.example.motionville.entity.video.Tag;
import org.example.motionville.entity.video.Video;
import org.example.motionville.repo.video.TagRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VideoTagService {

    private final VideoRepository videoRepository;
    private final TagRepository tagRepository;

    public VideoTagService(VideoRepository videoRepository, TagRepository tagRepository) {
        this.videoRepository = videoRepository;
        this.tagRepository = tagRepository;
    }

    @Transactional
    public void addTag(Long videoId, Long tagId) {
        Video video = findVideo(videoId);
        Tag tag = findTag(tagId);
        List<Tag> tags = video.getTags();
        if (tags == null) {
            tags = new ArrayList<>();
            video.setTags(tags);
        }
        if (tags.stream().anyMatch(existing -> existing.getId().equals(tagId))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tag is already assigned to this video");
        }
        video.addTag(tag);
        videoRepository.save(video);
    }

    @Transactional
    public void removeTag(Long videoId, Long tagId) {
        Video video = findVideo(videoId);
        Tag tag = findTag(tagId);
        if (video.getTags() == null
                || video.getTags().stream().noneMatch(existing -> existing.getId().equals(tagId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag is not assigned to this video");
        }
        video.removeTag(tag);
        videoRepository.save(video);
    }

    public List<TagResponse> getTags(Long videoId) {
        Video video = findVideo(videoId);
        if (video.getTags() == null) return List.of();
        return video.getTags().stream()
                .map(tag -> new TagResponse(tag.getId(), tag.getName()))
                .toList();
    }

    private Video findVideo(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Video not found"));
    }

    private Tag findTag(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag not found"));
    }
}
