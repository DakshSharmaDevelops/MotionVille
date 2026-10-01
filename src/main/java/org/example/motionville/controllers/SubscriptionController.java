package org.example.motionville.controllers;

import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.UserResponse;
import org.example.motionville.services.SubscriptionService;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/channels/{channelId}/subscribe")
    public ResponseEntity<Void> subscribe(
            @PathVariable Long channelId,
            @RequestParam Long userId) {

        subscriptionService.subscribe(userId, channelId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/channels/{channelId}/subscribe")
    public ResponseEntity<Void> unsubscribe(
            @PathVariable Long channelId,
            @RequestParam Long userId) {

        subscriptionService.unsubscribe(userId, channelId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/channels/{channelId}/subscribers")
    public ResponseEntity<List<UserResponse>> getSubscribers(
            @PathVariable Long channelId) {

        return ResponseEntity.ok(
                subscriptionService.getChannelSubscribers(channelId)
        );
    }

    @GetMapping("/users/{userId}/subscriptions")
    public ResponseEntity<List<ChannelResponse>> getUserSubscriptions(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                subscriptionService.getUserSubscriptions(userId)
        );
    }

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