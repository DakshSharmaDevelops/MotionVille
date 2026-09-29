package org.example.motionville.video.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.motionville.video.VideoVisibility;
import org.springframework.web.multipart.MultipartFile;

public class VideoUploadForm {
    @NotBlank
    @Size(max = 180)
    private String title;

    @Size(max = 5000)
    private String description;

    @Size(max = 80)
    private String category;

    @Size(max = 500)
    private String tags;

    private VideoVisibility visibility = VideoVisibility.PUBLIC;

    private MultipartFile file;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public VideoVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(VideoVisibility visibility) {
        this.visibility = visibility;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }
}
