package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessageDelivered;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityMessageDeliveredRepository extends JpaRepository<ActivityMessageDelivered, Long> {

    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    long countByMessageIdAndUserIdNot(Long messageId, Long userId);
}
