package org.example.motionville.video.web;

import jakarta.validation.Valid;
import org.example.motionville.account.MotionVilleUserDetails;
import org.example.motionville.channel.ChannelService;
import org.example.motionville.engagement.EngagementService;
import org.example.motionville.video.Video;
import org.example.motionville.video.VideoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class VideoController {
    private final VideoService videos;
    private final ChannelService channels;
    private final EngagementService engagement;

    public VideoController(VideoService videos, ChannelService channels, EngagementService engagement) {
        this.videos = videos;
        this.channels = channels;
        this.engagement = engagement;
    }

    @GetMapping("/videos/upload")
    public String uploadPage(@AuthenticationPrincipal MotionVilleUserDetails user, Model model) {
        if (channels.findByOwner(user.getId()) == null) {
            return "redirect:/channels/create";
        }
        model.addAttribute("form", new VideoUploadForm());
        return "video/upload";
    }

    @PostMapping("/videos/upload")
    public String upload(
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @Valid @ModelAttribute("form") VideoUploadForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "video/upload";
        }
        try {
            Video video = videos.upload(user.getId(), form);
            return "redirect:/videos/" + video.getId();
        } catch (VideoService.ChannelMissingException exception) {
            return "redirect:/channels/create";
        } catch (VideoService.InvalidVideoException exception) {
            model.addAttribute("uploadError", exception.getMessage());
            return "video/upload";
        }
    }

    @GetMapping("/videos/{videoId}")
    public String view(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            Model model) {
        Video video = videos.view(videoId, user == null ? null : user.getId());
        model.addAttribute("video", video);
        model.addAttribute("comments", engagement.commentsForVideo(videoId));
        model.addAttribute("likeCount", engagement.countLikes(videoId));
        model.addAttribute("liked", user != null && engagement.isLiked(user.getId(), videoId));
        model.addAttribute("ownVideo", user != null
                && video.getChannel().getOwner().getId().equals(user.getId()));
        model.addAttribute("currentUserId", user == null ? null : user.getId());
        model.addAttribute("commentForm", new CommentForm());
        return "video/view";
    }

    @PostMapping("/videos/{videoId}/comments")
    public String comment(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @ModelAttribute CommentForm form) {
        engagement.addComment(videoId, user.getId(), form.body());
        return "redirect:/videos/" + videoId;
    }

    @PostMapping("/videos/{videoId}/like")
    public String like(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user) {
        engagement.setLike(videoId, user.getId(), true);
        return "redirect:/videos/" + videoId;
    }

    @PostMapping("/videos/{videoId}/unlike")
    public String unlike(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user) {
        engagement.setLike(videoId, user.getId(), false);
        return "redirect:/videos/" + videoId;
    }

    @PostMapping("/videos/{videoId}/delete")
    public String deleteVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user) {
        videos.softDelete(videoId, user.getId());
        return "redirect:/";
    }

    @GetMapping("/videos/{videoId}/edit")
    public String editPage(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            Model model) {
        Video video = videos.getForOwner(videoId, user.getId());
        model.addAttribute("video", video);
        model.addAttribute("form", new VideoEditForm(
                video.getTitle(),
                video.getDescription(),
                video.getCategory(),
                video.getTags(),
                video.getVisibility()));
        return "video/edit";
    }

    @PostMapping("/videos/{videoId}/edit")
    public String updateVideo(
            @PathVariable Long videoId,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @Valid @ModelAttribute("form") VideoEditForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("video", videos.getForOwner(videoId, user.getId()));
            return "video/edit";
        }
        videos.update(videoId, user.getId(), form);
        return "redirect:/videos/" + videoId;
    }

    @PostMapping("/comments/{commentId}/delete")
    public String deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal MotionVilleUserDetails user) {
        Long videoId = engagement.deleteComment(commentId, user.getId());
        return "redirect:/videos/" + videoId;
    }

    @PostMapping("/channels/{channelId}/subscribe")
    public String subscribe(
            @PathVariable Long channelId,
            @AuthenticationPrincipal MotionVilleUserDetails user) {
        engagement.setSubscription(channelId, user.getId(), true);
        return "redirect:/channels/" + channelId;
    }

    @PostMapping("/channels/{channelId}/unsubscribe")
    public String unsubscribe(
            @PathVariable Long channelId,
            @AuthenticationPrincipal MotionVilleUserDetails user) {
        engagement.setSubscription(channelId, user.getId(), false);
        return "redirect:/channels/" + channelId;
    }

    public record CommentForm(String body) {
        public CommentForm() {
            this("");
        }
    }
}
