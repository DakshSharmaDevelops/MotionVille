package org.example.motionville.services.live;

import org.example.motionville.dto.UserResponse;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.services.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LiveBroadcastServiceTest {
    private LiveMediaServer media;
    private LiveBroadcastService service;
    private UserResponse user;
    @BeforeEach void setup() {
        var channels = mock(ChannelRepository.class);
        var users = mock(AppUserService.class);
        media = mock(LiveMediaServer.class);
        user = new UserResponse(); user.setId(5L);
        when(users.login("creator", "password")).thenReturn(user);
        var owner = new AppUser(); owner.setId(5L);
        var channel = new Channel(); channel.setChannelId(2L); channel.setOwner(owner); channel.setName("Creator");
        when(channels.findById(2L)).thenReturn(Optional.of(channel));
        service = new LiveBroadcastService(channels, users, media, "rtmp://localhost:1935", "http://localhost:8888");
    }
    @Test void credentialsRequiredToPublishAndControlButNotView() {
        var studio = service.create(2L, "Demo", "creator", "password");
        String path = "live-" + studio.broadcast().id();
        String secret = studio.streamKey().split("pass=")[1];
        assertTrue(service.authorize("publish", path, secret));
        assertFalse(service.authorize("publish", path, "wrong"));
        assertFalse(service.authorize("publish", path, null));
        assertFalse(service.authorize("api", path, secret));
        assertTrue(service.authorize("read", path, null));
        assertFalse(studio.broadcast().playbackUrl().contains(secret));
        assertThrows(ResponseStatusException.class, () -> service.end(studio.broadcast().id(), "wrong"));
        verify(media, never()).delete(anyString());
        service.end(studio.broadcast().id(), studio.managementToken());
        assertFalse(service.authorize("publish", path, secret));
        assertFalse(service.authorize("read", path, null));
        assertThrows(ResponseStatusException.class, () -> service.get(studio.broadcast().id()));
    }
    @Test void ownerMismatchCannotCreateBroadcast() {
        user.setId(99L);
        var failure = assertThrows(ResponseStatusException.class, () -> service.create(2L, "Demo", "creator", "password"));
        assertEquals(403, failure.getStatusCode().value());
        verifyNoInteractions(media);
    }
    @Test void reopeningDoesNotCreateDuplicateAndOnlyReadyStreamsAreListed() {
        var first = service.create(2L, "Demo", "creator", "password");
        var again = service.create(2L, "Other title", "creator", "password");
        assertEquals(first.broadcast().id(), again.broadcast().id());
        verify(media, times(1)).create(anyString());
        assertTrue(service.list().isEmpty());
        when(media.ready(anyString())).thenReturn(true);
        assertEquals("LIVE", service.get(first.broadcast().id()).status());
        assertEquals(1, service.list().size());
        when(media.ready(anyString())).thenReturn(false);
        assertEquals("WAITING", service.get(first.broadcast().id()).status());
    }
    @Test void unavailableMediaServerDoesNotLeaveSessionBehind() {
        doThrow(new IllegalStateException("offline")).when(media).create(anyString());
        assertThrows(IllegalStateException.class, () -> service.create(2L, "Demo", "creator", "password"));
        assertTrue(service.list().isEmpty());
    }
    @Test void failedEndCanBeRetried() {
        var studio = service.create(2L, "Demo", "creator", "password");
        doThrow(new IllegalStateException("offline")).when(media).delete(anyString());
        assertThrows(IllegalStateException.class, () -> service.end(studio.broadcast().id(), studio.managementToken()));
        assertNotNull(service.get(studio.broadcast().id()));
    }
}
