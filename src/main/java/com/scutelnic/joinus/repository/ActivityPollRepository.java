package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityPoll;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityPollRepository extends JpaRepository<ActivityPoll, Long> {

    @EntityGraph(attributePaths = {"creator"})
    List<ActivityPoll> findByActivityIdOrderByCreatedAtDesc(Long activityId);

    @EntityGraph(attributePaths = {"creator"})
    Optional<ActivityPoll> findByIdAndActivityId(Long pollId, Long activityId);

    long deleteByActivityId(Long activityId);
}
