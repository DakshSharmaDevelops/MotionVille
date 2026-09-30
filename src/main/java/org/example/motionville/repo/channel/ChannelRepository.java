package org.example.motionville.repo.channel;

import org.example.motionville.entity.channel.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
}
