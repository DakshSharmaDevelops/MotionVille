package org.example.motionville.controllers.channel;

import org.example.motionville.dto.channel.ChannelResponse;
import org.example.motionville.dto.account.UserResponse;
import org.example.motionville.services.channel.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(
            SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @PostMapping("/channels/{channelId}/subscribe")
    public ResponseEntity<Void> subscribe(
            @PathVariable Long channelId,
            @RequestParam Long userId) {

        subscriptionService.subscribe(userId, channelId);

        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @DeleteMapping("/channels/{channelId}/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable Long channelId,
            @RequestParam Long userId) {

        subscriptionService.unsubscribe(userId, channelId);

        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isChannelOwner(#channelId, authentication)")
    @GetMapping("/channels/{channelId}/subscribers")
    public ResponseEntity<List<UserResponse>> getSubscribers(
            @PathVariable Long channelId) {

        return ResponseEntity.ok(
                subscriptionService.getChannelSubscribers(channelId)
        );
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @GetMapping("/users/{userId}/subscriptions")
    public ResponseEntity<List<ChannelResponse>> getUserSubscriptions(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                subscriptionService.getUserSubscriptions(userId)
        );
    }

    @PreAuthorize("hasRole('ADMIN') or @authorizationService.isUserOwner(#userId, authentication)")
    @GetMapping("/channels/{channelId}/subscription-status")
    public ResponseEntity<Boolean> getSubscriptionStatus(
            @PathVariable Long channelId,
            @RequestParam Long userId) {

        return ResponseEntity.ok(
                subscriptionService.isSubscribed(
                        userId,
                        channelId
                )
        );
    }

    @GetMapping("/channels/{channelId}/subscriber-count")
    public ResponseEntity<Long> getSubscriberCount(
            @PathVariable Long channelId) {

        return ResponseEntity.ok(
                subscriptionService.getSubscriberCount(channelId)
        );
    }
}