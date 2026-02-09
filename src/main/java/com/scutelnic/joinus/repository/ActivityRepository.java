package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
}
