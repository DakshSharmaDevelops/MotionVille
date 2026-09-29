package org.example.motionville.channel.web;

import jakarta.validation.Valid;
import org.example.motionville.account.MotionVilleUserDetails;
import org.example.motionville.channel.Channel;
import org.example.motionville.channel.ChannelConflictException;
import org.example.motionville.channel.ChannelService;
import org.example.motionville.video.VideoService;
import org.example.motionville.engagement.EngagementService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ChannelController {
    private final ChannelService channels;
    private final VideoService videos;
    private final EngagementService engagement;

    public ChannelController(ChannelService channels, VideoService videos, EngagementService engagement) {
        this.channels = channels;
        this.videos = videos;
        this.engagement = engagement;
    }

    @GetMapping("/channels/create")
    public String createPage(@AuthenticationPrincipal MotionVilleUserDetails user, Model model) {
        Channel existing = channels.findByOwner(user.getId());
        if (existing != null) {
            return "redirect:/channels/" + existing.getId();
        }
        model.addAttribute("form", new ChannelForm("", ""));
        return "channel/create";
    }

    @PostMapping("/channels/create")
    public String create(
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @Valid @ModelAttribute("form") ChannelForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            return "channel/create";
        }
        try {
            Channel channel = channels.create(user.getId(), form);
            return "redirect:/channels/" + channel.getId();
        } catch (ChannelConflictException exception) {
            model.addAttribute("channelError", exception.getMessage());
            return "channel/create";
        }
    }

    @GetMapping("/channels/{channelId}")
    public String view(
            @PathVariable Long channelId,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            Model model) {
        Channel channel = channels.get(channelId);
        model.addAttribute("channel", channel);
        model.addAttribute("form", new ChannelForm(channel.getName(), channel.getDescription()));
        model.addAttribute("videos", videos.findChannelVideos(channelId, PageRequest.of(0, 24)));
        model.addAttribute("subscriberCount", engagement.countSubscribers(channelId));
        model.addAttribute("subscribed", user != null && engagement.isSubscribed(user.getId(), channelId));
        model.addAttribute("ownChannel", user != null && channel.getOwner().getId().equals(user.getId()));
        return "channel/view";
    }

    @PostMapping("/channels/{channelId}/edit")
    public String update(
            @PathVariable Long channelId,
            @AuthenticationPrincipal MotionVilleUserDetails user,
            @Valid @ModelAttribute("form") ChannelForm form,
            BindingResult bindingResult,
            Model model) {
        if (bindingResult.hasErrors()) {
            Channel channel = channels.get(channelId);
            model.addAttribute("channel", channel);
            model.addAttribute("videos", videos.findChannelVideos(channelId, PageRequest.of(0, 24)));
            model.addAttribute("subscriberCount", engagement.countSubscribers(channelId));
            model.addAttribute("subscribed", false);
            model.addAttribute("ownChannel", true);
            return "channel/view";
        }
        channels.update(channelId, user.getId(), form);
        return "redirect:/channels/" + channelId;
    }
}
