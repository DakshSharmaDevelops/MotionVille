package org.example.motionville.channel;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.example.motionville.account.UserAccount;
import org.example.motionville.account.UserAccountRepository;
import org.example.motionville.channel.web.ChannelForm;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelService {
    private final ChannelRepository channels;
    private final UserAccountRepository accounts;

    public ChannelService(ChannelRepository channels, UserAccountRepository accounts) {
        this.channels = channels;
        this.accounts = accounts;
    }

    @Transactional
    public Channel create(Long ownerId, ChannelForm form) {
        if (channels.existsByOwnerId(ownerId)) {
            throw new ChannelConflictException("You already have a channel.");
        }
        UserAccount owner = accounts.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found."));
        String base = form.name().trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (base.isBlank()) {
            base = "channel";
        }
        String handle = "@" + base;
        if (channels.existsByHandle(handle)) {
            handle += "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        owner.makeCreator();
        return channels.save(new Channel(owner, form.name().trim(), handle, Objects.toString(form.description(), "").trim()));
    }

    @Transactional(readOnly = true)
    public Channel get(Long channelId) {
        return channels.findById(channelId)
                .orElseThrow(() -> new ResourceNotFoundException("Channel not found."));
    }

    @Transactional
    public Channel update(Long channelId, Long actorId, ChannelForm form) {
        Channel channel = get(channelId);
        if (!channel.getOwner().getId().equals(actorId)) {
            throw new AccessDeniedException("Only the channel owner can update this channel.");
        }
        channel.update(form.name().trim(), Objects.toString(form.description(), "").trim());
        return channel;
    }

    @Transactional(readOnly = true)
    public Channel findByOwner(Long ownerId) {
        return channels.findByOwnerId(ownerId).orElse(null);
    }
}
