package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityMessageRepository extends JpaRepository<ActivityMessage, Long> {

    @EntityGraph(attributePaths = {"sender", "activity"})
    Optional<ActivityMessage> findWithSenderAndActivityById(Long id);

    @EntityGraph(attributePaths = {"sender"})
    List<ActivityMessage> findByActivityIdOrderByCreatedAtDescIdDesc(Long activityId, Pageable pageable);

    @EntityGraph(attributePaths = {"sender", "activity"})
    List<ActivityMessage> findByActivityIdAndIdIn(Long activityId, List<Long> ids);
}
