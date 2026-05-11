package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMemoryEntry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityMemoryEntryRepository extends JpaRepository<ActivityMemoryEntry, Long> {

    @EntityGraph(attributePaths = {"author", "photos"})
    List<ActivityMemoryEntry> findByActivityIdOrderByCreatedAtDesc(Long activityId);

    @EntityGraph(attributePaths = {"author", "photos"})
    List<ActivityMemoryEntry> findByActivityIdOrderByCreatedAtAsc(Long activityId);

    long deleteByActivityId(Long activityId);
}
