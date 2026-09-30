package org.example.motionville.controllers;
import org.example.motionville.entity.video.Video;
import org.example.motionville.entity.video.VideoAsset;
import org.example.motionville.services.videoService.VideoService;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/")
public class VideoController {

        private final VideoService videoService;

        public VideoController(VideoService videoService) {
            this.videoService = videoService;
        }

        @GetMapping
    public List<Video> getAllVideos() {
            return videoService.getAllVideos();
        }
        @GetMapping("/{id}")
        public Video getVideosById(@PathVariable Long id) {
            return videoService.getVideoById(id);
        }

    @PostMapping
    public void postVideo(@RequestBody Video video) {
        videoService.postVideo(video);
    }
   @DeleteMapping("/delete/{id}")
   public void deleteVideoById(@PathVariable Long id) {
            videoService.deleteVideo(id);
    }
    @GetMapping("/play/{id}")
    public List<VideoAsset> playVideo(@PathVariable Long id) {
        return videoService.playVideo(id);
    }

}
