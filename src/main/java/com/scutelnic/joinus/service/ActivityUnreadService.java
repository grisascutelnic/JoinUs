package com.scutelnic.joinus.service;

import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityMessageRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ActivityUnreadService {

    private final ActivityMessageRepository activityMessageRepository;
    private final ActivityService activityService;
    private final ActivityParticipationService participationService;
    private final UserService userService;

    public ActivityUnreadService(ActivityMessageRepository activityMessageRepository,
                                 ActivityService activityService,
                                 ActivityParticipationService participationService,
                                 UserService userService) {
        this.activityMessageRepository = activityMessageRepository;
        this.activityService = activityService;
        this.participationService = participationService;
        this.userService = userService;
    }

    public long countUnreadActivities(String userEmail) {
        return getUnreadCountsByActivityForUser(userEmail).size();
    }

    public Map<Long, Long> getUnreadCountsByActivityForUser(String userEmail) {
        User user = resolveUser(userEmail);
        if (user == null) {
            return Map.of();
        }
        List<Long> accessibleActivityIds = resolveAccessibleActivityIds(user);
        return buildUnreadCounts(user.getId(), accessibleActivityIds);
    }

    public Map<Long, Long> getUnreadCountsByActivityForUser(String userEmail, Collection<Long> requestedActivityIds) {
        User user = resolveUser(userEmail);
        if (user == null) {
            return Map.of();
        }
        List<Long> accessible = filterRequestedToAccessible(user, requestedActivityIds);
        return buildUnreadCounts(user.getId(), accessible);
    }

    public Map<Long, LocalDateTime> getLatestUnreadMessageByActivityForUser(String userEmail,
                                                                             Collection<Long> requestedActivityIds) {
        User user = resolveUser(userEmail);
        if (user == null) {
            return Map.of();
        }
        List<Long> accessible = filterRequestedToAccessible(user, requestedActivityIds);
        return buildLatestUnread(user.getId(), accessible);
    }

    public Map<Long, Long> getUnreadAnnouncementCountsByActivityForUser(String userEmail,
                                                                         Collection<Long> requestedActivityIds) {
        User user = resolveUser(userEmail);
        if (user == null) {
            return Map.of();
        }
        List<Long> accessible = filterRequestedToAccessible(user, requestedActivityIds);
        return buildUnreadAnnouncementCounts(user.getId(), accessible);
    }

    private User resolveUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return null;
        }
        return userService.findByEmail(userEmail).orElse(null);
    }

    private List<Long> filterRequestedToAccessible(User user, Collection<Long> requestedActivityIds) {
        if (requestedActivityIds == null || requestedActivityIds.isEmpty()) {
            return List.of();
        }
        Set<Long> allowed = new LinkedHashSet<>(resolveAccessibleActivityIds(user));
        List<Long> filtered = new ArrayList<>();
        for (Long activityId : requestedActivityIds) {
            if (activityId == null || !allowed.contains(activityId)) {
                continue;
            }
            filtered.add(activityId);
        }
        return filtered;
    }

    private List<Long> resolveAccessibleActivityIds(User user) {
        List<Activity> authored = activityService.getByCreator(user.getId());
        List<Activity> approved = participationService.getApprovedActivitiesForUser(user.getEmail());

        Set<Long> uniqueIds = new LinkedHashSet<>();
        for (Activity activity : authored) {
            if (activity != null && activity.getId() != null) {
                uniqueIds.add(activity.getId());
            }
        }
        for (Activity activity : approved) {
            if (activity != null && activity.getId() != null) {
                uniqueIds.add(activity.getId());
            }
        }

        return new ArrayList<>(uniqueIds);
    }

    private Map<Long, Long> buildUnreadCounts(Long userId, List<Long> activityIds) {
        if (userId == null || activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new LinkedHashMap<>();
        List<ActivityMessageRepository.ActivityUnreadSummaryProjection> rows =
                activityMessageRepository.findUnreadSummaryByActivityIdsAndUserId(activityIds, userId);
        for (ActivityMessageRepository.ActivityUnreadSummaryProjection row : rows) {
            if (row.getActivityId() == null || row.getUnreadCount() == null) {
                continue;
            }
            if (row.getUnreadCount() > 0) {
                counts.put(row.getActivityId(), row.getUnreadCount());
            }
        }
        return counts;
    }

    private Map<Long, LocalDateTime> buildLatestUnread(Long userId, List<Long> activityIds) {
        if (userId == null || activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, LocalDateTime> latestByActivity = new LinkedHashMap<>();
        List<ActivityMessageRepository.ActivityUnreadSummaryProjection> rows =
                activityMessageRepository.findUnreadSummaryByActivityIdsAndUserId(activityIds, userId);
        for (ActivityMessageRepository.ActivityUnreadSummaryProjection row : rows) {
            if (row.getActivityId() == null || row.getLatestMessageAt() == null) {
                continue;
            }
            latestByActivity.put(row.getActivityId(), row.getLatestMessageAt());
        }
        return latestByActivity;
    }

    private Map<Long, Long> buildUnreadAnnouncementCounts(Long userId, List<Long> activityIds) {
        if (userId == null || activityIds == null || activityIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new LinkedHashMap<>();
        List<ActivityMessageRepository.ActivityUnreadSummaryProjection> rows =
                activityMessageRepository.findUnreadAnnouncementSummaryByActivityIdsAndUserId(activityIds, userId);
        for (ActivityMessageRepository.ActivityUnreadSummaryProjection row : rows) {
            if (row.getActivityId() == null || row.getUnreadCount() == null) {
                continue;
            }
            if (row.getUnreadCount() > 0) {
                counts.put(row.getActivityId(), row.getUnreadCount());
            }
        }
        return counts;
    }
}
