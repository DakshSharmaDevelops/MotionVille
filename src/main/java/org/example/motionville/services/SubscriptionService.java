package org.example.motionville.services;

import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.UserResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.channel.Subscription;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.channel.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final AppUserRepository appUserRepository;
    private final ChannelRepository channelRepository;
    private final NotificationCreationService notificationCreationService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            AppUserRepository appUserRepository,
            ChannelRepository channelRepository,
            NotificationCreationService notificationCreationService) {

        this.subscriptionRepository = subscriptionRepository;
        this.appUserRepository = appUserRepository;
        this.channelRepository = channelRepository;
        this.notificationCreationService = notificationCreationService;
    }

    @Transactional
    public void subscribe(Long userId, Long channelId) {

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        Channel channel = channelRepository.findById(channelId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        if (subscriptionRepository
                .existsBySubscriber_IdAndChannel_ChannelId(
                        userId,
                        channelId)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Already subscribed to this channel"
            );
        }

        Subscription subscription = new Subscription();

        subscription.setSubscriber(user);
        subscription.setChannel(channel);
        subscription.setSubscribedAt(Instant.now());

        subscriptionRepository.save(subscription);
        notificationCreationService.notifyNewSubscriber(channel, user);
    }

    @Transactional
    public void unsubscribe(Long userId, Long channelId) {

        if (!appUserRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        if (!channelRepository.existsById(channelId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Channel not found"
            );
        }

        Subscription subscription =
                subscriptionRepository
                        .findBySubscriber_IdAndChannel_ChannelId(
                                userId,
                                channelId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Subscription not found"
                                )
                        );

        subscriptionRepository.delete(subscription);
    }

    public boolean isSubscribed(Long userId, Long channelId) {

        if (!appUserRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        if (!channelRepository.existsById(channelId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Channel not found"
            );
        }

        return subscriptionRepository
                .existsBySubscriber_IdAndChannel_ChannelId(
                        userId,
                        channelId
                );
    }

    public List<ChannelResponse> getUserSubscriptions(Long userId) {

        if (!appUserRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        List<Subscription> subscriptions =
                subscriptionRepository.findBySubscriber_Id(userId);

        List<ChannelResponse> responses = new ArrayList<>();

        for (Subscription subscription : subscriptions) {

            Channel channel = subscription.getChannel();

            ChannelResponse response = new ChannelResponse();

            response.setId(channel.getChannelId());
            response.setOwnerId(channel.getOwner().getId());
            response.setHandle(channel.getHandle());
            response.setName(channel.getName());
            response.setDescription(channel.getDescription());
            response.setBannerUrl(channel.getBannerUrl());
            response.setCreatedAt(channel.getCreatedAt());

            responses.add(response);
        }

        return responses;
    }

    public List<UserResponse> getChannelSubscribers(Long channelId) {

        if (!channelRepository.existsById(channelId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Channel not found"
            );
        }

        List<Subscription> subscriptions =
                subscriptionRepository.findByChannel_ChannelId(channelId);

        List<UserResponse> responses = new ArrayList<>();

        for (Subscription subscription : subscriptions) {

            AppUser user = subscription.getSubscriber();

            UserResponse response = new UserResponse();

            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setDisplayName(user.getDisplayName());
            response.setAvatarUrl(user.getAvatarUrl());
            response.setCreatedAt(user.getCreatedAt());
            response.setUpdatedAt(user.getUpdatedAt());

            responses.add(response);
        }

        return responses;
    }

    public long getSubscriberCount(Long channelId) {

        if (!channelRepository.existsById(channelId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Channel not found"
            );
        }

        return subscriptionRepository.countByChannel_ChannelId(channelId);
    }
}