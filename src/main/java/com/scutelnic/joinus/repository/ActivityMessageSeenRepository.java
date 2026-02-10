package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessageSeen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityMessageSeenRepository extends JpaRepository<ActivityMessageSeen, Long> {

    long countByMessageId(Long messageId);

    long countByMessageIdAndUserIdNot(Long messageId, Long userId);

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    @EntityGraph(attributePaths = {"user"})
    List<ActivityMessageSeen> findByMessageIdOrderBySeenAtAsc(Long messageId);

    @EntityGraph(attributePaths = {"user"})
    List<ActivityMessageSeen> findByMessageIdAndUserIdNotOrderBySeenAtAsc(Long messageId, Long userId);

    @EntityGraph(attributePaths = {"user", "message", "message.sender"})
    List<ActivityMessageSeen> findByMessageActivityIdAndMessageIdInOrderByMessageIdAscSeenAtAsc(Long activityId, List<Long> messageIds);

    Optional<ActivityMessageSeen> findByMessageIdAndUserId(Long messageId, Long userId);
}
