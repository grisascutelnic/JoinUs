package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessageDelivered;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface ActivityMessageDeliveredRepository extends JpaRepository<ActivityMessageDelivered, Long> {

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    long countByMessageIdAndUserIdNot(Long messageId, Long userId);

    @Modifying
    @Transactional
    @Query(value = """
            insert into activity_message_delivered (message_id, user_id, delivered_at)
            values (:messageId, :userId, :deliveredAt)
            on conflict on constraint uq_message_delivered_user do nothing
            """, nativeQuery = true)
    int insertIgnoreDuplicate(@Param("messageId") Long messageId,
                              @Param("userId") Long userId,
                              @Param("deliveredAt") LocalDateTime deliveredAt);
}
