package org.example.motionville.services;

import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.DevelopmentChannelRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class DevelopmentChannelService {

    private final AppUserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final boolean enabled;

    public DevelopmentChannelService(
            AppUserRepository userRepository,
            ChannelRepository channelRepository,
            @Value("${motionville.dev-channel-bootstrap-enabled:false}") boolean enabled) {
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.enabled = enabled;
    }

    @Transactional
    public ChannelResponse createChannel(DevelopmentChannelRequest request) {
        if (!enabled) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Development channel bootstrap is disabled"
            );
        }

        String secret = UUID.randomUUID().toString();
        AppUser owner = new AppUser();
        owner.setUsername("dev-" + secret);
        owner.setEmail("dev-" + secret + "@example.test");
        owner.setPassword(secret);
        owner.setPasswordHash(secret);
        owner.setDisplayName(request.name().trim());
        owner = userRepository.save(owner);

        Channel channel = new Channel();
        channel.setOwner(owner);
        channel.setName(request.name().trim());
        channel.setHandle(request.handle().trim());
        channel.setDescription(request.description());
        channel.setBannerUrl(request.bannerUrl());
        channel = channelRepository.save(channel);

        ChannelResponse response = new ChannelResponse();
        response.setId(channel.getChannelId());
        response.setOwnerId(owner.getId());
        response.setName(channel.getName());
        response.setHandle(channel.getHandle());
        response.setDescription(channel.getDescription());
        response.setBannerUrl(channel.getBannerUrl());
        response.setCreatedAt(channel.getCreatedAt());
        return response;
    }
}
