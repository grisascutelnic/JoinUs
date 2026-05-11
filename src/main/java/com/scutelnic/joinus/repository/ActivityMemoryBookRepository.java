package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMemoryBook;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityMemoryBookRepository extends JpaRepository<ActivityMemoryBook, Long> {

    @EntityGraph(attributePaths = {"activity", "activity.creator"})
    Optional<ActivityMemoryBook> findByActivityId(Long activityId);

    @EntityGraph(attributePaths = {"activity", "activity.creator"})
    List<ActivityMemoryBook> findByPublicUpdatedAtIsNotNullOrderByPublicUpdatedAtDesc();

    long deleteByActivityId(Long activityId);
}
