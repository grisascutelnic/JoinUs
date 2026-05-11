package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.ActivityMemoryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityMemoryPhotoRepository extends JpaRepository<ActivityMemoryPhoto, Long> {

    long deleteByEntryActivityId(Long activityId);
}
