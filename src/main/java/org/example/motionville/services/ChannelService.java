package org.example.motionville.services;

import org.example.motionville.dto.ChannelCreateRequest;
import org.example.motionville.dto.ChannelResponse;
import org.example.motionville.dto.ChannelUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final AppUserRepository appUserRepository;

    public ChannelService(
            ChannelRepository channelRepository,
            AppUserRepository appUserRepository) {

        this.channelRepository = channelRepository;
        this.appUserRepository = appUserRepository;
    }

    public ChannelResponse createChannel(
            ChannelCreateRequest request) {

        if (channelRepository.existsByHandle(request.getHandle())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Handle already exists"
            );
        }

        AppUser owner = appUserRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        Channel channel = new Channel();

        channel.setOwner(owner);
        channel.setHandle(request.getHandle());
        channel.setName(request.getName());
        channel.setDescription(request.getDescription());
        channel.setBannerUrl(request.getBannerUrl());

        Channel savedChannel = channelRepository.save(channel);

        return convertToResponse(savedChannel);
    }

    public ChannelResponse getChannelById(Long id) {

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        return convertToResponse(channel);
    }

    public List<ChannelResponse> getAllChannels() {

        List<Channel> channels = channelRepository.findAll();

        List<ChannelResponse> responses = new ArrayList<>();

        for (Channel channel : channels) {
            responses.add(convertToResponse(channel));
        }

        return responses;
    }

    public ChannelResponse updateChannel(
            Long id,
            ChannelUpdateRequest request) {

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        if (channelRepository.existsByHandleAndChannelIdNot(
                request.getHandle(),
                id)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Handle already exists"
            );
        }

        channel.setHandle(request.getHandle());
        channel.setName(request.getName());
        channel.setDescription(request.getDescription());
        channel.setBannerUrl(request.getBannerUrl());

        Channel updatedChannel = channelRepository.save(channel);

        return convertToResponse(updatedChannel);
    }

    public void deleteChannel(Long id) {

        Channel channel = channelRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Channel not found"
                        )
                );

        channelRepository.delete(channel);
    }

    public List<ChannelResponse> getChannelsByUserId(Long userId) {

        appUserRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        List<Channel> channels =
                channelRepository.findByOwner_Id(userId);

        List<ChannelResponse> responses = new ArrayList<>();

        for (Channel channel : channels) {
            responses.add(convertToResponse(channel));
        }

        return responses;
    }

    private ChannelResponse convertToResponse(Channel channel) {

        ChannelResponse response = new ChannelResponse();

        response.setId(channel.getChannelId());
        response.setOwnerId(channel.getOwner().getId());
        response.setHandle(channel.getHandle());
        response.setName(channel.getName());
        response.setDescription(channel.getDescription());
        response.setBannerUrl(channel.getBannerUrl());
        response.setCreatedAt(channel.getCreatedAt());

        return response;
    }
}