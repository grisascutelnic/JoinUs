package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessageReaction;
import com.scutelnic.joinus.entity.ActivityMessageReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActivityMessageReactionRepository extends JpaRepository<ActivityMessageReaction, Long> {

    Optional<ActivityMessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

    List<ActivityMessageReaction> findByMessageIdInAndUserId(List<Long> messageIds, Long userId);

    @Query("""
            select r.message.id as messageId, r.reactionType as reactionType, count(r) as total
            from ActivityMessageReaction r
            where r.message.id in :messageIds
            group by r.message.id, r.reactionType
            """)
    List<MessageReactionCountView> countGroupedByMessageIds(@Param("messageIds") List<Long> messageIds);

    interface MessageReactionCountView {
        Long getMessageId();

        ActivityMessageReactionType getReactionType();

        long getTotal();
    }
}
