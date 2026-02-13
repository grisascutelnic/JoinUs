package com.scutelnic.joinus.service;

import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.ActivityParticipation;
import com.scutelnic.joinus.entity.ParticipationStatus;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityParticipationRepository;
import com.scutelnic.joinus.repository.ActivityRepository;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ActivityParticipationService {

    private final ActivityRepository activityRepository;
    private final ActivityParticipationRepository participationRepository;
    private final UserRepository userRepository;

    public ActivityParticipationService(ActivityRepository activityRepository,
                                        ActivityParticipationRepository participationRepository,
                                        UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ActivityParticipation requestParticipation(Long activityId, String userEmail) {
        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);

        if (isCreator(activity, user)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Creatorul este deja participant");
        }

        return participationRepository.findByActivityIdAndUserId(activityId, user.getId())
                .map(existing -> updateExistingRequest(existing))
                .orElseGet(() -> createRequest(activity, user));
    }

    @Transactional
    public void approveRequest(Long activityId, Long requestId, String organizerEmail) {
        Activity activity = requireActivity(activityId);
        User organizer = requireUserByEmail(organizerEmail);
        requireOrganizer(activity, organizer);

        ActivityParticipation request = participationRepository.findByIdAndActivityId(requestId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu a fost gasita"));

        if (request.getStatus() == ParticipationStatus.APPROVED) {
            return;
        }

        long approvedCount = participationRepository.countByActivityIdAndStatus(activityId, ParticipationStatus.APPROVED);
        if (approvedCount >= activity.getCapacity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacitatea activitatii a fost atinsa");
        }

        request.setStatus(ParticipationStatus.APPROVED);
        request.setRespondedAt(LocalDateTime.now());
        participationRepository.save(request);
    }

    @Transactional
    public void rejectRequest(Long activityId, Long requestId, String organizerEmail) {
        Activity activity = requireActivity(activityId);
        User organizer = requireUserByEmail(organizerEmail);
        requireOrganizer(activity, organizer);

        ActivityParticipation request = participationRepository.findByIdAndActivityId(requestId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cererea nu a fost gasita"));

        if (request.getStatus() == ParticipationStatus.REJECTED || request.getStatus() == ParticipationStatus.BLOCKED) {
            return;
        }

        int denialCount = incrementDenialCount(request);
        request.setStatus(denialCount >= 2 ? ParticipationStatus.BLOCKED : ParticipationStatus.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        participationRepository.save(request);
    }

    @Transactional
    public void excludeParticipant(Long activityId, Long requestId, String organizerEmail) {
        Activity activity = requireActivity(activityId);
        User organizer = requireUserByEmail(organizerEmail);
        requireOrganizer(activity, organizer);

        ActivityParticipation request = participationRepository.findByIdAndActivityId(requestId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participantul nu a fost gasit"));

        if (request.getStatus() != ParticipationStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doar participantii acceptati pot fi exclusi");
        }

        int denialCount = incrementDenialCount(request);
        request.setStatus(denialCount >= 2 ? ParticipationStatus.BLOCKED : ParticipationStatus.EXCLUDED);
        request.setRespondedAt(LocalDateTime.now());
        participationRepository.save(request);
    }

    @Transactional(readOnly = true)
    public boolean canAccessChat(Long activityId, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return false;
        }

        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);
        if (isCreator(activity, user)) {
            return true;
        }

        return participationRepository.existsByActivityIdAndUserIdAndStatus(activityId, user.getId(), ParticipationStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public boolean isCreator(Long activityId, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return false;
        }
        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);
        return isCreator(activity, user);
    }

    @Transactional(readOnly = true)
    public ParticipationStatus getParticipationStatus(Long activityId, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return null;
        }

        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);
        if (isCreator(activity, user)) {
            return ParticipationStatus.APPROVED;
        }

        return participationRepository.findByActivityIdAndUserId(activityId, user.getId())
                .map(ActivityParticipation::getStatus)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ActivityParticipation> getPendingRequestsForOrganizer(Long activityId, String organizerEmail) {
        if (organizerEmail == null || organizerEmail.isBlank()) {
            return List.of();
        }
        Activity activity = requireActivity(activityId);
        User organizer = requireUserByEmail(organizerEmail);
        if (!isCreator(activity, organizer)) {
            return List.of();
        }
        return participationRepository.findByActivityIdAndStatusOrderByRequestedAtAsc(activityId, ParticipationStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public List<ActivityParticipation> getApprovedParticipantsForOrganizer(Long activityId, String organizerEmail) {
        if (organizerEmail == null || organizerEmail.isBlank()) {
            return List.of();
        }
        Activity activity = requireActivity(activityId);
        User organizer = requireUserByEmail(organizerEmail);
        if (!isCreator(activity, organizer)) {
            return List.of();
        }
        return participationRepository.findByActivityIdAndStatusOrderByRequestedAtAsc(activityId, ParticipationStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public List<ActivityParticipation> getApprovedParticipantsForViewer(Long activityId, String viewerEmail) {
        if (viewerEmail == null || viewerEmail.isBlank()) {
            return List.of();
        }
        if (!canAccessChat(activityId, viewerEmail)) {
            return List.of();
        }
        return participationRepository.findByActivityIdAndStatusOrderByRequestedAtAsc(activityId, ParticipationStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    public boolean canRequestParticipation(Long activityId, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return false;
        }
        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);
        if (isCreator(activity, user)) {
            return false;
        }

        ActivityParticipation participation = participationRepository.findByActivityIdAndUserId(activityId, user.getId()).orElse(null);
        if (participation == null) {
            return true;
        }

        ParticipationStatus status = participation.getStatus();
        if (status == ParticipationStatus.PENDING || status == ParticipationStatus.APPROVED || status == ParticipationStatus.BLOCKED) {
            return false;
        }

        return getDenialCount(participation) < 2;
    }

    @Transactional(readOnly = true)
    public List<Activity> getApprovedActivitiesForUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return List.of();
        }
        User user = requireUserByEmail(userEmail);
        return participationRepository
                .findByUserIdAndStatusOrderByRequestedAtDesc(user.getId(), ParticipationStatus.APPROVED)
                .stream()
                .map(ActivityParticipation::getActivity)
                .filter(Objects::nonNull)
                .toList();
    }

    private ActivityParticipation updateExistingRequest(ActivityParticipation existing) {
        if (existing.getStatus() == ParticipationStatus.PENDING || existing.getStatus() == ParticipationStatus.APPROVED) {
            return existing;
        }

        if (existing.getStatus() == ParticipationStatus.BLOCKED || getDenialCount(existing) >= 2) {
            existing.setStatus(ParticipationStatus.BLOCKED);
            participationRepository.save(existing);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu poti participa la aceasta activitate.");
        }

        existing.setStatus(ParticipationStatus.PENDING);
        existing.setRequestedAt(LocalDateTime.now());
        existing.setRespondedAt(null);
        return participationRepository.save(existing);
    }

    private ActivityParticipation createRequest(Activity activity, User user) {
        ActivityParticipation request = new ActivityParticipation();
        request.setActivity(activity);
        request.setUser(user);
        request.setStatus(ParticipationStatus.PENDING);
        request.setRequestedAt(LocalDateTime.now());
        return participationRepository.save(request);
    }

    private int incrementDenialCount(ActivityParticipation request) {
        int denialCount = getDenialCount(request) + 1;
        request.setDenialCount(denialCount);
        return denialCount;
    }

    private int getDenialCount(ActivityParticipation request) {
        if (request.getDenialCount() != null) {
            return request.getDenialCount();
        }
        if (request.getStatus() == ParticipationStatus.BLOCKED) {
            return 2;
        }
        if (request.getStatus() == ParticipationStatus.REJECTED || request.getStatus() == ParticipationStatus.EXCLUDED) {
            return 1;
        }
        return 0;
    }

    private void requireOrganizer(Activity activity, User user) {
        if (!isCreator(activity, user)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar organizatorul poate procesa cereri");
        }
    }

    private boolean isCreator(Activity activity, User user) {
        return activity.getCreator() != null && activity.getCreator().getId().equals(user.getId());
    }

    private Activity requireActivity(Long activityId) {
        return activityRepository.findWithCreatorById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activitatea nu a fost gasita"));
    }

    private User requireUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
