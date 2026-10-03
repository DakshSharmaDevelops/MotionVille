package org.example.motionville.services;

import org.example.motionville.dto.ChannelCreateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChannelServiceTest {

    private ChannelRepository channelRepository;
    private AppUserRepository appUserRepository;
    private ChannelService service;
    private AppUser owner;

    @BeforeEach
    void setUp() {
        channelRepository = mock(ChannelRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        service = new ChannelService(channelRepository, appUserRepository);
        owner = new AppUser();
        owner.setId(4L);
        when(appUserRepository.findById(4L)).thenReturn(Optional.of(owner));
    }

    @Test
    void rejectsCreatingSecondChannelForUser() {
        when(channelRepository.existsByOwner_Id(4L)).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createChannel(channelRequest()));

        assertEquals(409, exception.getStatusCode().value());
        verify(channelRepository, never()).save(any());
    }

    @Test
    void createsFirstChannelForUser() {
        when(channelRepository.existsByOwner_Id(4L)).thenReturn(false);
        when(channelRepository.existsByHandle("@creator")).thenReturn(false);
        when(channelRepository.save(any())).thenAnswer(invocation -> {
            Channel channel = invocation.getArgument(0);
            channel.setChannelId(12L);
            return channel;
        });

        var response = service.createChannel(channelRequest());

        assertEquals(12L, response.getId());
        assertEquals(4L, response.getOwnerId());
    }

    private ChannelCreateRequest channelRequest() {
        ChannelCreateRequest request = new ChannelCreateRequest();
        request.setOwnerId(4L);
        request.setName("Creator");
        request.setHandle("@creator");
        return request;
    }
}
