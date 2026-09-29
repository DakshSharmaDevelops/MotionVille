package org.example.motionville.video.web;

import org.example.motionville.account.MotionVilleUserDetails;
import org.example.motionville.channel.ChannelService;
import org.example.motionville.engagement.EngagementService;
import org.example.motionville.video.VideoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {
    private final VideoService videos;
    private final ChannelService channels;
    private final EngagementService engagement;

    public HomeController(
            VideoService videos,
            ChannelService channels,
            EngagementService engagement) {
        this.videos = videos;
        this.channels = channels;
        this.engagement = engagement;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "home") String feed,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            Model model) {
        int pageNumber = Math.max(0, Math.min(page, 1_000_000));
        PageRequest pageRequest = PageRequest.of(pageNumber, 12);
        var localVideos = feed.equals("trending") && q.isBlank() && category.isBlank()
                ? videos.findPublicTrending(pageRequest)
                : videos.findPublicVideos(q, category, pageRequest);
        model.addAttribute("videos", localVideos);
        model.addAttribute("query", q);
        model.addAttribute("category", category);
        model.addAttribute("feed", feed);
        model.addAttribute("myChannel", user == null ? null : channels.findByOwner(user.getId()));
        model.addAttribute(
                "subscriptions",
                user == null ? java.util.List.of() : engagement.subscriptionsForUser(user.getId()));
        return "home";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        var subscriptions = engagement.subscriptionsForUser(user.getId());
        var channelIds = subscriptions.stream().map(subscription -> subscription.getChannel().getId()).toList();
        model.addAttribute(
                "videos",
                videos.findVideosFromChannels(channelIds, PageRequest.of(Math.max(0, Math.min(page, 1_000_000)), 12)));
        model.addAttribute("subscriptions", subscriptions);
        model.addAttribute("activePage", "subscriptions");
        return "subscriptions";
    }

}
