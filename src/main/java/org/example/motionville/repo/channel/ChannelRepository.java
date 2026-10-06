package org.example.motionville.repo.channel;

import org.example.motionville.entity.channel.Channel;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    boolean existsByHandle(String handle);

    boolean existsByHandleAndChannelIdNot(String handle, Long id);

    boolean existsByOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"owner"})
    Optional<Channel> findByOwner_Id(Long ownerId);

    @EntityGraph(attributePaths = {"owner"})
    @Query("""
            select c from Channel c
            where :search = ''
               or lower(c.name) like lower(concat('%', :search, '%'))
               or lower(c.handle) like lower(concat('%', :search, '%'))
               or lower(coalesce(c.description, '')) like lower(concat('%', :search, '%'))
            """)
    List<Channel> searchChannels(@Param("search") String search);
}
