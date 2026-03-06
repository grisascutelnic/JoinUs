package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.Notification;
import com.scutelnic.joinus.entity.NotificationType;
import com.scutelnic.joinus.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);

    long countByRecipientAndIsReadFalse(User recipient);

    Optional<Notification> findByIdAndRecipient(Long id, User recipient);

    boolean existsByRecipientAndType(User recipient, NotificationType type);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.recipient = :recipient and n.isRead = false")
    int markAllAsReadByRecipient(@Param("recipient") User recipient);
}
