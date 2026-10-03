package org.example.motionville.repo.channel;

import org.example.motionville.entity.channel.Channel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByHandle(String handle);

    boolean existsByHandleAndChannelIdNot(String handle, Long id);

    boolean existsByOwner_Id(Long ownerId);

    List<Channel> findByOwner_Id(Long ownerId);
}