package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.Activity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @EntityGraph(attributePaths = "creator")
    Optional<Activity> findWithCreatorById(Long id);

    List<Activity> findAllByCreatorIdOrderByCreatedAtDesc(Long creatorId);
}
