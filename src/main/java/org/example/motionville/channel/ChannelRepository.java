package org.example.motionville.channel;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRepository extends JpaRepository<Channel, Long> {
    @EntityGraph(attributePaths = "owner")
    Optional<Channel> findByOwnerId(Long ownerId);

    boolean existsByOwnerId(Long ownerId);

    boolean existsByHandle(String handle);

    @Override
    @EntityGraph(attributePaths = "owner")
    Optional<Channel> findById(Long id);
}
