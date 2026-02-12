package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityParticipation;
import com.scutelnic.joinus.entity.ParticipationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityParticipationRepository extends JpaRepository<ActivityParticipation, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<ActivityParticipation> findByActivityIdAndStatusOrderByRequestedAtAsc(Long activityId, ParticipationStatus status);

    Optional<ActivityParticipation> findByActivityIdAndUserId(Long activityId, Long userId);

    Optional<ActivityParticipation> findByIdAndActivityId(Long id, Long activityId);

    boolean existsByActivityIdAndUserIdAndStatus(Long activityId, Long userId, ParticipationStatus status);

    long countByActivityIdAndStatus(Long activityId, ParticipationStatus status);
}
