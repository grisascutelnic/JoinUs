package com.scutelnic.joinus.service;

import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;

    public ActivityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public Activity create(Activity activity) {
        return activityRepository.save(activity);
    }

    public Activity create(String title,
                           String description,
                           LocalDate date,
                           LocalTime time,
                           String location,
                           String address,
                           int capacity,
                           String category,
                           String tags,
                           String imageUrl,
                           User creator) {
        Activity activity = new Activity();
        activity.setTitle(title);
        activity.setDescription(description);
        activity.setDate(date);
        activity.setTime(time);
        activity.setLocation(location);
        activity.setAddress(address);
        activity.setCapacity(capacity);
        activity.setCategory(category);
        activity.setTags(tags);
        activity.setImageUrl(imageUrl);
        activity.setCreator(creator);
        activity.setCreatedAt(LocalDateTime.now());
        return create(activity);
    }

    public List<Activity> getAll() {
        return activityRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public List<Activity> getRecent(int limit) {
        return activityRepository.findAll(PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();
    }

    public Optional<Activity> getById(Long id) {
        return activityRepository.findWithCreatorById(id);
    }

    public List<Activity> getByCreator(Long creatorId) {
        return activityRepository.findAllByCreatorIdOrderByCreatedAtDesc(creatorId);
    }
}
