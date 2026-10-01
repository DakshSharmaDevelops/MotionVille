package org.example.motionville.controllers;

import jakarta.validation.Valid;
import org.example.motionville.dto.TagRequest;
import org.example.motionville.dto.TagResponse;
import org.example.motionville.dto.VideoResponse;
import org.example.motionville.services.videoService.TagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "${motionville.frontend-origin:http://localhost:5173}")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @PostMapping
    public ResponseEntity<TagResponse> create(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(request));
    }

    @GetMapping
    public List<TagResponse> getAll() {
        return tagService.getAll();
    }

    @GetMapping("/{tagId}")
    public TagResponse get(@PathVariable Long tagId) {
        return tagService.get(tagId);
    }

    @PutMapping("/{tagId}")
    public TagResponse update(
            @PathVariable Long tagId,
            @Valid @RequestBody TagRequest request) {
        return tagService.update(tagId, request);
    }

    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> delete(@PathVariable Long tagId) {
        tagService.delete(tagId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{tagId}/videos")
    public List<VideoResponse> getVideos(@PathVariable Long tagId) {
        return tagService.getVideos(tagId);
    }
}
