package com.scutelnic.joinus.service;

import com.scutelnic.joinus.dto.chat.ChatMessageResponse;
import com.scutelnic.joinus.dto.chat.MessageSeenSummaryResponse;
import com.scutelnic.joinus.dto.chat.SeenUpdateEvent;
import com.scutelnic.joinus.dto.chat.SeenUserResponse;
import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.ActivityMessage;
import com.scutelnic.joinus.entity.ActivityMessageSeen;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityMessageDeliveredRepository;
import com.scutelnic.joinus.repository.ActivityMessageRepository;
import com.scutelnic.joinus.repository.ActivityMessageSeenRepository;
import com.scutelnic.joinus.repository.ActivityRepository;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityChatService {

    private static final int MAX_HISTORY_LIMIT = 100;

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityMessageRepository messageRepository;
    private final ActivityMessageDeliveredRepository deliveredRepository;
    private final ActivityMessageSeenRepository seenRepository;
    private final ActivityParticipationService participationService;

    public ActivityChatService(ActivityRepository activityRepository,
                               UserRepository userRepository,
                               ActivityMessageRepository messageRepository,
                               ActivityMessageDeliveredRepository deliveredRepository,
                               ActivityMessageSeenRepository seenRepository,
                               ActivityParticipationService participationService) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.deliveredRepository = deliveredRepository;
        this.seenRepository = seenRepository;
        this.participationService = participationService;
    }

    public List<ChatMessageResponse> getRecentMessages(Long activityId, int requestedLimit, String userEmail) {
        requireChatAccess(activityId, userEmail);
        requireActivity(activityId);
        int limit = Math.max(1, Math.min(MAX_HISTORY_LIMIT, requestedLimit));
        List<ActivityMessage> messages = messageRepository.findByActivityIdOrderByCreatedAtDescIdDesc(
                activityId,
                PageRequest.of(0, limit)
        );
        Collections.reverse(messages);
        return messages.stream()
                .map(this::toMessageResponse)
                .toList();
    }

    public ChatMessageResponse sendMessage(Long activityId, String senderEmail, String content) {
        String normalizedContent = normalizeContent(content);
        requireChatAccess(activityId, senderEmail);
        Activity activity = requireActivity(activityId);
        User sender = requireUserByEmail(senderEmail);

        ActivityMessage message = new ActivityMessage();
        message.setActivity(activity);
        message.setSender(sender);
        message.setContent(normalizedContent);
        ActivityMessage saved = messageRepository.save(message);
        return toMessageResponse(saved);
    }

    public SeenUpdateEvent markDelivered(Long activityId, Long messageId, String userEmail) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }

        requireChatAccess(activityId, userEmail);
        User user = requireUserByEmail(userEmail);
        ActivityMessage message = messageRepository.findWithSenderAndActivityById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getActivity().getId().equals(activityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to activity");
        }

        Long senderId = message.getSender().getId();
        if (!senderId.equals(user.getId())) {
            deliveredRepository.insertIgnoreDuplicate(messageId, user.getId(), LocalDateTime.now());
        }

        long deliveredCount = deliveredRepository.countByMessageIdAndUserIdNot(messageId, senderId);
        long seenCount = seenRepository.countByMessageIdAndUserIdNot(messageId, senderId);
        return new SeenUpdateEvent(messageId, deliveredCount, seenCount);
    }

    public SeenUpdateEvent markSeen(Long activityId, Long messageId, String userEmail) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }

        requireChatAccess(activityId, userEmail);
        User user = requireUserByEmail(userEmail);
        ActivityMessage message = messageRepository.findWithSenderAndActivityById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getActivity().getId().equals(activityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to activity");
        }

        Long senderId = message.getSender().getId();

        if (!senderId.equals(user.getId())) {
            deliveredRepository.insertIgnoreDuplicate(messageId, user.getId(), LocalDateTime.now());
            seenRepository.insertIgnoreDuplicate(messageId, user.getId(), LocalDateTime.now());
        }

        long deliveredCount = deliveredRepository.countByMessageIdAndUserIdNot(messageId, senderId);
        long seenCount = seenRepository.countByMessageIdAndUserIdNot(messageId, senderId);
        return new SeenUpdateEvent(messageId, deliveredCount, seenCount);
    }

    public List<SeenUserResponse> getSeenUsers(Long messageId, String userEmail) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }

        ActivityMessage message = messageRepository.findWithSenderAndActivityById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        requireChatAccess(message.getActivity().getId(), userEmail);

        Long senderId = message.getSender().getId();
        return seenRepository.findByMessageIdAndUserIdNotOrderBySeenAtAsc(message.getId(), senderId)
                .stream()
                .map(seen -> new SeenUserResponse(
                        seen.getUser().getId(),
                        seen.getUser().getFullName(),
                        seen.getSeenAt()
                ))
                .toList();
    }

    public List<MessageSeenSummaryResponse> getSeenUsersForMessages(Long activityId, List<Long> messageIds, String userEmail) {
        requireChatAccess(activityId, userEmail);
        requireActivity(activityId);
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }

        List<ActivityMessage> messages = messageRepository.findByActivityIdAndIdIn(activityId, messageIds);
        if (messages.isEmpty()) {
            return List.of();
        }

        Map<Long, ActivityMessage> messagesById = new LinkedHashMap<>();
        messages.stream()
                .sorted(Comparator.comparing(ActivityMessage::getCreatedAt).thenComparing(ActivityMessage::getId))
                .forEach(message -> messagesById.put(message.getId(), message));

        List<Long> validMessageIds = new ArrayList<>(messagesById.keySet());
        Map<Long, List<SeenUserResponse>> seenByMessage = new LinkedHashMap<>();
        for (Long messageId : validMessageIds) {
            seenByMessage.put(messageId, new ArrayList<>());
        }

        List<ActivityMessageSeen> seenEntries = seenRepository
                .findByMessageActivityIdAndMessageIdInOrderByMessageIdAscSeenAtAsc(activityId, validMessageIds);

        for (ActivityMessageSeen seen : seenEntries) {
            ActivityMessage message = seen.getMessage();
            if (message == null || message.getSender() == null) {
                continue;
            }
            if (seen.getUser().getId().equals(message.getSender().getId())) {
                continue;
            }
            List<SeenUserResponse> viewers = seenByMessage.get(message.getId());
            if (viewers == null) {
                continue;
            }
            viewers.add(new SeenUserResponse(
                    seen.getUser().getId(),
                    seen.getUser().getFullName(),
                    seen.getSeenAt()
            ));
        }

        return validMessageIds.stream()
                .map(id -> new MessageSeenSummaryResponse(id, List.copyOf(seenByMessage.get(id))))
                .toList();
    }

    private ChatMessageResponse toMessageResponse(ActivityMessage message) {
        long deliveredCount = deliveredRepository.countByMessageIdAndUserIdNot(message.getId(), message.getSender().getId());
        long seenCount = seenRepository.countByMessageIdAndUserIdNot(message.getId(), message.getSender().getId());
        return new ChatMessageResponse(
                message.getId(),
                message.getActivity().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt(),
                deliveredCount,
                seenCount
        );
    }

    private Activity requireActivity(Long activityId) {
        return activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
    }

    private User requireUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private String normalizeContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        String normalized = content.trim();
        if (normalized.length() > 1500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is too long");
        }
        return normalized;
    }

    private void requireChatAccess(Long activityId, String userEmail) {
        if (!participationService.canAccessChat(activityId, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la chat pentru aceasta activitate");
        }
    }
}
