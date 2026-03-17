package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessage;
import com.scutelnic.joinus.entity.ActivityMessageType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ActivityMessageRepository extends JpaRepository<ActivityMessage, Long> {

    interface ActivityUnreadSummaryProjection {
        Long getActivityId();

        Long getUnreadCount();

        LocalDateTime getLatestMessageAt();
    }

    @EntityGraph(attributePaths = {"sender", "activity"})
    Optional<ActivityMessage> findWithSenderAndActivityById(Long id);

    @EntityGraph(attributePaths = {"sender"})
    List<ActivityMessage> findByActivityIdOrderByCreatedAtDescIdDesc(Long activityId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    List<ActivityMessage> findByActivityIdAndMessageTypeOrderByCreatedAtDescIdDesc(Long activityId,
                                                                                    ActivityMessageType messageType,
                                                                                    Pageable pageable);

    @EntityGraph(attributePaths = {"sender"})
    List<ActivityMessage> findByActivityIdAndMessageTypeOrderByCreatedAtAscIdAsc(Long activityId,
                                                                                  ActivityMessageType messageType);

    boolean existsByActivityIdAndMessageType(Long activityId, ActivityMessageType messageType);

    @EntityGraph(attributePaths = {"sender", "activity"})
    List<ActivityMessage> findByActivityIdAndIdIn(Long activityId, List<Long> ids);

        @Query(value = """
                        select m.activity_id as activityId,
                                     count(*) as unreadCount,
                                     max(m.created_at) as latestMessageAt
                        from activity_messages m
                        where m.activity_id in (:activityIds)
                            and m.sender_id <> :userId
                            and not exists (
                                    select 1
                                    from activity_message_seen s
                                    where s.message_id = m.id
                                        and s.user_id = :userId
                            )
                        group by m.activity_id
                        """, nativeQuery = true)
        List<ActivityUnreadSummaryProjection> findUnreadSummaryByActivityIdsAndUserId(@Param("activityIds") List<Long> activityIds,
                                                                                                                                                                    @Param("userId") Long userId);

    @Query(value = """
                    select m.activity_id as activityId,
                                 count(*) as unreadCount,
                                 max(m.created_at) as latestMessageAt
                    from activity_messages m
                    where m.activity_id in (:activityIds)
                        and coalesce(m.message_type, 'CHAT') = 'ANNOUNCEMENT'
                        and m.sender_id <> :userId
                        and not exists (
                                select 1
                                from activity_message_seen s
                                where s.message_id = m.id
                                    and s.user_id = :userId
                        )
                    group by m.activity_id
                    """, nativeQuery = true)
    List<ActivityUnreadSummaryProjection> findUnreadAnnouncementSummaryByActivityIdsAndUserId(@Param("activityIds") List<Long> activityIds,
                                                                                                @Param("userId") Long userId);

    long deleteByActivityId(Long activityId);
}
