package org.example.motionville.services.channel;

import org.example.motionville.services.notification.NotificationCreationService;

import org.example.motionville.dto.channel.ChannelResponse;
import org.example.motionville.dto.account.UserResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.entity.channel.Subscription;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.channel.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ChannelRepository channelRepository;

    @Mock
    private NotificationCreationService notificationCreationService;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private AppUser owner;
    private AppUser subscriber;
    private Channel channel;

    @BeforeEach
    void setUp() {
        owner = new AppUser();
        owner.setId(1L);
        owner.setUsername("owner");

        subscriber = new AppUser();
        subscriber.setId(2L);
        subscriber.setUsername("subscriber");

        channel = new Channel();
        channel.setChannelId(10L);
        channel.setName("Owner's Channel");
        channel.setHandle("@owner");
        channel.setOwner(owner);
    }

    @Test
    @DisplayName("Subscribe succeeds for different user and notifies channel owner")
    void subscribe_success() {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(subscriber));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(subscriptionRepository.existsBySubscriber_IdAndChannel_ChannelId(2L, 10L)).thenReturn(false);

        subscriptionService.subscribe(2L, 10L);

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(notificationCreationService).notifyNewSubscriber(channel, subscriber);
    }

    @Test
    @DisplayName("Subscribe fails with 400 Bad Request when user tries to subscribe to their own channel")
    void subscribe_selfSubscription_throwsBadRequest() {
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> subscriptionService.subscribe(1L, 10L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot subscribe to your own channel", ex.getReason());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Subscribe fails with 409 Conflict when already subscribed")
    void subscribe_duplicate_throwsConflict() {
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(subscriber));
        when(channelRepository.findById(10L)).thenReturn(Optional.of(channel));
        when(subscriptionRepository.existsBySubscriber_IdAndChannel_ChannelId(2L, 10L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> subscriptionService.subscribe(2L, 10L));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unsubscribe removes subscription when found")
    void unsubscribe_success() {
        Subscription subscription = new Subscription();
        subscription.setId(100L);
        subscription.setSubscriber(subscriber);
        subscription.setChannel(channel);

        when(appUserRepository.existsById(2L)).thenReturn(true);
        when(channelRepository.existsById(10L)).thenReturn(true);
        when(subscriptionRepository.findBySubscriber_IdAndChannel_ChannelId(2L, 10L))
                .thenReturn(Optional.of(subscription));

        subscriptionService.unsubscribe(2L, 10L);

        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    @DisplayName("isSubscribed returns true when subscription exists")
    void isSubscribed_returnsTrue() {
        when(appUserRepository.existsById(2L)).thenReturn(true);
        when(channelRepository.existsById(10L)).thenReturn(true);
        when(subscriptionRepository.existsBySubscriber_IdAndChannel_ChannelId(2L, 10L)).thenReturn(true);
        assertTrue(subscriptionService.isSubscribed(2L, 10L));
    }

    @Test
    @DisplayName("getSubscriberCount returns count for channel")
    void getSubscriberCount_returnsCount() {
        when(channelRepository.existsById(10L)).thenReturn(true);
        when(subscriptionRepository.countByChannel_ChannelId(10L)).thenReturn(42L);

        assertEquals(42L, subscriptionService.getSubscriberCount(10L));
    }
}
