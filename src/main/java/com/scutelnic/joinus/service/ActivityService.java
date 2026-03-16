package com.scutelnic.joinus.service;

import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityRepository;
import com.scutelnic.joinus.repository.ActivityMessageDeliveredRepository;
import com.scutelnic.joinus.repository.ActivityMessageReactionRepository;
import com.scutelnic.joinus.repository.ActivityMessageRepository;
import com.scutelnic.joinus.repository.ActivityMessageSeenRepository;
import com.scutelnic.joinus.repository.ActivityParticipationRepository;
import com.scutelnic.joinus.repository.ActivityPollOptionRepository;
import com.scutelnic.joinus.repository.ActivityPollRepository;
import com.scutelnic.joinus.repository.ActivityPollVoteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ActivityParticipationRepository activityParticipationRepository;
    private final ActivityMessageRepository activityMessageRepository;
    private final ActivityMessageSeenRepository activityMessageSeenRepository;
    private final ActivityMessageDeliveredRepository activityMessageDeliveredRepository;
    private final ActivityMessageReactionRepository activityMessageReactionRepository;
    private final ActivityPollRepository activityPollRepository;
    private final ActivityPollOptionRepository activityPollOptionRepository;
    private final ActivityPollVoteRepository activityPollVoteRepository;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityParticipationRepository activityParticipationRepository,
                           ActivityMessageRepository activityMessageRepository,
                           ActivityMessageSeenRepository activityMessageSeenRepository,
                           ActivityMessageDeliveredRepository activityMessageDeliveredRepository,
                           ActivityMessageReactionRepository activityMessageReactionRepository,
                           ActivityPollRepository activityPollRepository,
                           ActivityPollOptionRepository activityPollOptionRepository,
                           ActivityPollVoteRepository activityPollVoteRepository) {
        this.activityRepository = activityRepository;
        this.activityParticipationRepository = activityParticipationRepository;
        this.activityMessageRepository = activityMessageRepository;
        this.activityMessageSeenRepository = activityMessageSeenRepository;
        this.activityMessageDeliveredRepository = activityMessageDeliveredRepository;
        this.activityMessageReactionRepository = activityMessageReactionRepository;
        this.activityPollRepository = activityPollRepository;
        this.activityPollOptionRepository = activityPollOptionRepository;
        this.activityPollVoteRepository = activityPollVoteRepository;
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

    public Page<Activity> getPage(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 30));
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return activityRepository.findAll(pageable);
    }

    public Optional<Activity> getById(Long id) {
        return activityRepository.findWithCreatorById(id);
    }

    public List<Activity> getByCreator(Long creatorId) {
        return activityRepository.findAllByCreatorIdOrderByCreatedAtDesc(creatorId);
    }

    @Transactional
    public void deleteActivityWithRelations(Long activityId) {
        activityMessageSeenRepository.deleteByMessageActivityId(activityId);
        activityMessageDeliveredRepository.deleteByMessageActivityId(activityId);
        activityMessageReactionRepository.deleteByMessageActivityId(activityId);
        activityMessageRepository.deleteByActivityId(activityId);
        activityParticipationRepository.deleteByActivityId(activityId);
        activityPollVoteRepository.deleteByPollActivityId(activityId);
        activityPollOptionRepository.deleteByPollActivityId(activityId);
        activityPollRepository.deleteByActivityId(activityId);
        activityRepository.deleteById(activityId);
    }
}
