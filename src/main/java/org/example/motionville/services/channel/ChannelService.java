package org.example.motionville.services.channel;

import org.example.motionville.dto.channel.ChannelCreateRequest;
import org.example.motionville.dto.channel.ChannelResponse;
import org.example.motionville.dto.channel.ChannelUpdateRequest;
import org.example.motionville.entity.account.AppUser;
import org.example.motionville.entity.channel.Channel;
import org.example.motionville.repo.account.AppUserRepository;
import org.example.motionville.repo.channel.ChannelRepository;
import org.example.motionville.repo.engagement.VideoViewRepository;
import org.example.motionville.repo.video.VideoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final AppUserRepository appUserRepository;
    private final VideoRepository videoRepository;
    private final VideoViewRepository videoViewRepository;

    public ChannelService(
            ChannelRepository channelRepository,
            AppUserRepository appUserRepository,
            VideoRepository videoRepository,
            VideoViewRepository videoViewRepository) {

        this.channelRepository = channelRepository;
        this.appUserRepository = appUserRepository;
        this.videoRepository = videoRepository;
        this.videoViewRepository = videoViewRepository;
    }
    public ChannelResponse createChannel(
            ChannelCreateRequest request) {

        AppUser owner = appUserRepository.findById(request.getOwnerId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );

        if (channelRepository.existsByOwner_Id(owner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Each user can create only one channel"
            );
        }

        if (channelRepository.existsByHandle(request.getHandle())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Handle already exists"
            );
        }

        Channel channel = new Channel();

        channel.setOwner(owner);
        channel.setHandle(request.getHandle());
        channel.setName(request.getName());
        channel.setDescription(request.getDescription());
        channel.setBannerUrl(request.getBannerUrl());
        channel.setProfileImageUrl(request.getProfileImageUrl());

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

    public List<ChannelResponse> getAllChannels(String search) {
        List<Channel> channels;
        if (search != null && !search.trim().isEmpty()) {
            channels = channelRepository.searchChannels(search.trim());
        } else {
            channels = channelRepository.findAll();
        }

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
        channel.setProfileImageUrl(request.getProfileImageUrl());

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

        Optional<Channel> channelOpt =
                channelRepository.findByOwner_Id(userId);

        List<ChannelResponse> responses = new ArrayList<>();

        channelOpt.ifPresent(channel -> responses.add(convertToResponse(channel)));

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
        String profileImg = channel.getProfileImageUrl();
        if ((profileImg == null || profileImg.isBlank()) && channel.getOwner() != null) {
            profileImg = channel.getOwner().getAvatarUrl();
        }
        response.setProfileImageUrl(profileImg);
        response.setCreatedAt(channel.getCreatedAt());
        response.setVideoCount(videoRepository.countByChannel_ChannelId(channel.getChannelId()));
        response.setViewCount(videoViewRepository.countByVideo_Channel_ChannelId(channel.getChannelId()));

        return response;
    }
}