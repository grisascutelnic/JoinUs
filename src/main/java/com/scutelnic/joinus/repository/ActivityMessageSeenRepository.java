package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessageSeen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Modifying
    @Transactional
    @Query(value = """
            insert into activity_message_seen (message_id, user_id, seen_at)
            values (:messageId, :userId, :seenAt)
            on conflict on constraint uq_message_seen_user do nothing
            """, nativeQuery = true)
    int insertIgnoreDuplicate(@Param("messageId") Long messageId,
                              @Param("userId") Long userId,
                              @Param("seenAt") LocalDateTime seenAt);
}
