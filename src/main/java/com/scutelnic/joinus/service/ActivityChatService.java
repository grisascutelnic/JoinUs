package com.scutelnic.joinus.service;

import com.scutelnic.joinus.dto.chat.ChatMessageResponse;
import com.scutelnic.joinus.dto.chat.AnnouncementResponse;
import com.scutelnic.joinus.dto.chat.MessageReactionSummaryResponse;
import com.scutelnic.joinus.dto.chat.MessageReactionUpdateEvent;
import com.scutelnic.joinus.dto.chat.MessageSeenSummaryResponse;
import com.scutelnic.joinus.dto.chat.PollOptionSummaryResponse;
import com.scutelnic.joinus.dto.chat.PollResponse;
import com.scutelnic.joinus.dto.chat.PollUpdateEvent;
import com.scutelnic.joinus.dto.chat.PollVoterResponse;
import com.scutelnic.joinus.dto.chat.SeenUpdateEvent;
import com.scutelnic.joinus.dto.chat.SeenUserResponse;
import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.ActivityMessage;
import com.scutelnic.joinus.entity.ActivityMessageReaction;
import com.scutelnic.joinus.entity.ActivityMessageReactionType;
import com.scutelnic.joinus.entity.ActivityMessageSeen;
import com.scutelnic.joinus.entity.ActivityMessageType;
import com.scutelnic.joinus.entity.ActivityPoll;
import com.scutelnic.joinus.entity.ActivityPollOption;
import com.scutelnic.joinus.entity.ActivityPollVote;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityMessageDeliveredRepository;
import com.scutelnic.joinus.repository.ActivityMessageReactionRepository;
import com.scutelnic.joinus.repository.ActivityMessageRepository;
import com.scutelnic.joinus.repository.ActivityMessageSeenRepository;
import com.scutelnic.joinus.repository.ActivityPollOptionRepository;
import com.scutelnic.joinus.repository.ActivityPollRepository;
import com.scutelnic.joinus.repository.ActivityPollVoteRepository;
import com.scutelnic.joinus.repository.ActivityRepository;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActivityChatService {

    private static final int MAX_HISTORY_LIMIT = 100;
    private static final int MAX_ANNOUNCEMENT_LENGTH = 1000;
    private static final String LEGACY_WELCOME_ANNOUNCEMENT = "Bun venit in grup! Foloseste acest tab pentru anunturi importante despre activitate.";
    private static final String DEFAULT_WELCOME_ANNOUNCEMENT = "Bun venit in grup! Mult succes la activitate si distractie placuta tuturor participantilor!";
    private static final int MAX_POLL_QUESTION_LENGTH = 280;
    private static final int MAX_POLL_OPTION_LENGTH = 160;
    private static final int MAX_POLL_OPTIONS = 10;
    private static final List<ActivityMessageReactionType> SUPPORTED_REACTIONS = List.of(
            ActivityMessageReactionType.LIKE,
            ActivityMessageReactionType.LOVE,
            ActivityMessageReactionType.LAUGH,
            ActivityMessageReactionType.WOW
    );

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityMessageRepository messageRepository;
    private final ActivityMessageDeliveredRepository deliveredRepository;
    private final ActivityMessageReactionRepository reactionRepository;
    private final ActivityMessageSeenRepository seenRepository;
    private final ActivityPollRepository pollRepository;
    private final ActivityPollOptionRepository pollOptionRepository;
    private final ActivityPollVoteRepository pollVoteRepository;
    private final ActivityParticipationService participationService;

    public ActivityChatService(ActivityRepository activityRepository,
                               UserRepository userRepository,
                               ActivityMessageRepository messageRepository,
                               ActivityMessageDeliveredRepository deliveredRepository,
                               ActivityMessageReactionRepository reactionRepository,
                               ActivityMessageSeenRepository seenRepository,
                               ActivityPollRepository pollRepository,
                               ActivityPollOptionRepository pollOptionRepository,
                               ActivityPollVoteRepository pollVoteRepository,
                               ActivityParticipationService participationService) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.deliveredRepository = deliveredRepository;
        this.reactionRepository = reactionRepository;
        this.seenRepository = seenRepository;
        this.pollRepository = pollRepository;
        this.pollOptionRepository = pollOptionRepository;
        this.pollVoteRepository = pollVoteRepository;
        this.participationService = participationService;
    }

    public List<PollResponse> getPolls(Long activityId, String userEmail) {
        requireChatAccess(activityId, userEmail);
        User currentUser = requireUserByEmail(userEmail);
        List<ActivityPoll> polls = pollRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
        if (polls.isEmpty()) {
            return List.of();
        }
        return buildPollResponses(polls, currentUser.getId());
    }

    @Transactional
    public void ensureDefaultWelcomeAnnouncement(Long activityId) {
        Activity activity = requireActivity(activityId);
        if (activity.getCreator() == null) {
            return;
        }

        List<ActivityMessage> existingAnnouncements = messageRepository
                .findByActivityIdAndMessageTypeOrderByCreatedAtAscIdAsc(activityId, ActivityMessageType.ANNOUNCEMENT);

        if (!existingAnnouncements.isEmpty()) {
            List<ActivityMessage> toUpdate = new ArrayList<>();
            for (ActivityMessage announcement : existingAnnouncements) {
                String content = announcement.getContent();
                if (content == null) {
                    continue;
                }
                if (!content.trim().equals(LEGACY_WELCOME_ANNOUNCEMENT)) {
                    continue;
                }
                announcement.setContent(DEFAULT_WELCOME_ANNOUNCEMENT);
                toUpdate.add(announcement);
            }
            if (!toUpdate.isEmpty()) {
                messageRepository.saveAll(toUpdate);
            }
            return;
        }

        ActivityMessage announcement = new ActivityMessage();
        announcement.setActivity(activity);
        announcement.setSender(activity.getCreator());
        announcement.setContent(DEFAULT_WELCOME_ANNOUNCEMENT);
        announcement.setMessageType(ActivityMessageType.ANNOUNCEMENT);
        messageRepository.save(announcement);
    }

    public List<AnnouncementResponse> getAnnouncements(Long activityId, String userEmail) {
        requireChatAccess(activityId, userEmail);
        ensureDefaultWelcomeAnnouncement(activityId);

        List<ActivityMessage> announcements = messageRepository
                .findByActivityIdAndMessageTypeOrderByCreatedAtAscIdAsc(activityId, ActivityMessageType.ANNOUNCEMENT);
        if (announcements.isEmpty()) {
            return List.of();
        }

        return announcements.stream()
                .map(this::toAnnouncementResponse)
                .toList();
    }

    public AnnouncementResponse createAnnouncement(Long activityId, String userEmail, String content) {
        User author = requireActivityAuthor(activityId, userEmail);
        Activity activity = requireActivity(activityId);

        ActivityMessage announcement = new ActivityMessage();
        announcement.setActivity(activity);
        announcement.setSender(author);
        announcement.setContent(normalizeAnnouncementContent(content));
        announcement.setMessageType(ActivityMessageType.ANNOUNCEMENT);

        ActivityMessage saved = messageRepository.save(announcement);
        return toAnnouncementResponse(saved);
    }

    @Transactional
    public PollUpdateEvent createPoll(Long activityId, String userEmail, String question, List<String> options) {
        requireChatAccess(activityId, userEmail);
        Activity activity = requireActivity(activityId);
        User creator = requireUserByEmail(userEmail);

        String normalizedQuestion = normalizePollQuestion(question);
        List<String> normalizedOptions = normalizePollOptions(options);

        ActivityPoll poll = new ActivityPoll();
        poll.setActivity(activity);
        poll.setCreator(creator);
        poll.setQuestion(normalizedQuestion);
        ActivityPoll savedPoll = pollRepository.save(poll);

        for (int i = 0; i < normalizedOptions.size(); i++) {
            ActivityPollOption option = new ActivityPollOption();
            option.setPoll(savedPoll);
            option.setText(normalizedOptions.get(i));
            option.setPosition(i);
            pollOptionRepository.save(option);
        }

        PollResponse response = getPollResponseById(activityId, savedPoll.getId(), creator.getId());
        return new PollUpdateEvent("created", savedPoll.getId(), response);
    }

    @Transactional
    public PollUpdateEvent votePoll(Long activityId, Long pollId, Long optionId, String userEmail) {
        if (pollId == null || optionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pollId and optionId are required");
        }

        requireChatAccess(activityId, userEmail);
        User voter = requireUserByEmail(userEmail);

        ActivityPoll poll = pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        ActivityPollOption option = pollOptionRepository.findByIdAndPollId(optionId, pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option does not belong to poll"));

        ActivityPollVote vote = pollVoteRepository.findByPollIdAndVoterId(pollId, voter.getId()).orElse(null);
        if (vote == null) {
            vote = new ActivityPollVote();
            vote.setPoll(poll);
            vote.setVoter(voter);
        }
        vote.setOption(option);
        vote.setVotedAt(LocalDateTime.now());
        pollVoteRepository.save(vote);

        PollResponse response = getPollResponseById(activityId, pollId, voter.getId());
        return new PollUpdateEvent("voted", pollId, response);
    }

    @Transactional
    public PollUpdateEvent editPoll(Long activityId, Long pollId, String question, String userEmail) {
        if (pollId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pollId is required");
        }

        requireChatAccess(activityId, userEmail);
        User actor = requireActivityAuthor(activityId, userEmail);

        ActivityPoll poll = pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        poll.setQuestion(normalizePollQuestion(question));
        pollRepository.save(poll);

        PollResponse response = getPollResponseById(activityId, pollId, actor.getId());
        return new PollUpdateEvent("updated", pollId, response);
    }

    @Transactional
    public PollUpdateEvent addPollOption(Long activityId, Long pollId, String optionText, String userEmail) {
        if (pollId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pollId is required");
        }

        requireChatAccess(activityId, userEmail);
        User actor = requireUserByEmail(userEmail);

        ActivityPoll poll = pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        List<ActivityPollOption> existing = pollOptionRepository.findByPollId(pollId);
        if (existing.size() >= MAX_POLL_OPTIONS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll already has maximum number of options");
        }

        String normalized = normalizePollOption(optionText);
        boolean duplicate = existing.stream()
                .anyMatch(option -> option.getText() != null && option.getText().trim().equalsIgnoreCase(normalized));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option already exists");
        }

        int nextPosition = existing.stream()
                .map(ActivityPollOption::getPosition)
                .max(Integer::compareTo)
                .orElse(-1) + 1;

        ActivityPollOption option = new ActivityPollOption();
        option.setPoll(poll);
        option.setText(normalized);
        option.setPosition(nextPosition);
        pollOptionRepository.save(option);

        PollResponse response = getPollResponseById(activityId, pollId, actor.getId());
        return new PollUpdateEvent("option_added", pollId, response);
    }

    @Transactional
    public PollUpdateEvent editPollOption(Long activityId,
                                          Long pollId,
                                          Long optionId,
                                          String optionText,
                                          String userEmail) {
        if (pollId == null || optionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pollId and optionId are required");
        }

        requireChatAccess(activityId, userEmail);
        User actor = requireActivityAuthor(activityId, userEmail);

        pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        ActivityPollOption option = pollOptionRepository.findByIdAndPollId(optionId, pollId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option does not belong to poll"));

        String normalized = normalizePollOption(optionText);
        List<ActivityPollOption> existing = pollOptionRepository.findByPollId(pollId);
        boolean duplicate = existing.stream()
                .anyMatch(current -> !current.getId().equals(optionId)
                        && current.getText() != null
                        && current.getText().trim().equalsIgnoreCase(normalized));
        if (duplicate) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option already exists");
        }

        option.setText(normalized);
        pollOptionRepository.save(option);

        PollResponse response = getPollResponseById(activityId, pollId, actor.getId());
        return new PollUpdateEvent("option_updated", pollId, response);
    }

    @Transactional
    public PollUpdateEvent deletePollOption(Long activityId, Long pollId, Long optionId, String userEmail) {
        if (pollId == null || optionId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pollId and optionId are required");
        }

        requireChatAccess(activityId, userEmail);
        User actor = requireActivityAuthor(activityId, userEmail);

        pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        List<ActivityPollOption> existing = pollOptionRepository.findByPollIdOrderByPositionAsc(pollId);
        if (existing.size() <= 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A poll must keep at least 2 options");
        }

        ActivityPollOption target = existing.stream()
                .filter(option -> option.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option does not belong to poll"));

        pollVoteRepository.deleteByOptionId(optionId);
        pollOptionRepository.delete(target);

        List<ActivityPollOption> remaining = pollOptionRepository.findByPollIdOrderByPositionAsc(pollId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setPosition(i);
        }
        pollOptionRepository.saveAll(remaining);

        PollResponse response = getPollResponseById(activityId, pollId, actor.getId());
        return new PollUpdateEvent("option_deleted", pollId, response);
    }

    @Transactional
    public PollUpdateEvent deletePoll(Long activityId, Long pollId, String userEmail) {
        if (pollId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pollId is required");
        }

        requireChatAccess(activityId, userEmail);
        requireActivityAuthor(activityId, userEmail);

        ActivityPoll poll = pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));

        pollVoteRepository.deleteByPollId(pollId);
        pollOptionRepository.deleteByPollId(pollId);
        pollRepository.delete(poll);

        return new PollUpdateEvent("deleted", pollId, null);
    }

    public List<ChatMessageResponse> getRecentMessages(Long activityId, int requestedLimit, String userEmail) {
        requireChatAccess(activityId, userEmail);
        requireActivity(activityId);
        User currentUser = requireUserByEmail(userEmail);
        int limit = Math.max(1, Math.min(MAX_HISTORY_LIMIT, requestedLimit));
        List<ActivityMessage> messages = messageRepository.findByActivityIdOrderByCreatedAtDescIdDesc(
            activityId,
            PageRequest.of(0, limit)
        ).stream()
                .filter(message -> message.getMessageType() == null || message.getMessageType() == ActivityMessageType.CHAT)
            .collect(Collectors.toCollection(ArrayList::new));
        Collections.reverse(messages);

        List<Long> messageIds = messages.stream().map(ActivityMessage::getId).toList();
        Map<Long, Map<ActivityMessageReactionType, Long>> reactionCountsByMessage = buildReactionCountsByMessage(messageIds);
        Map<Long, ActivityMessageReactionType> currentUserReactionsByMessage = buildCurrentUserReactionsByMessage(
            messageIds,
            currentUser.getId()
        );

        return messages.stream()
            .map(message -> toMessageResponse(
                message,
                reactionCountsByMessage,
                currentUserReactionsByMessage
            ))
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
        message.setMessageType(ActivityMessageType.CHAT);
        ActivityMessage saved = messageRepository.save(message);
        return toMessageResponse(saved, Map.of(), Map.of());
    }

    public MessageReactionUpdateEvent toggleReaction(Long activityId, Long messageId, String reactionTypeValue, String userEmail) {
        if (messageId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messageId is required");
        }

        ActivityMessageReactionType reactionType = ActivityMessageReactionType.fromValue(reactionTypeValue);
        if (reactionType == null || !SUPPORTED_REACTIONS.contains(reactionType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported reaction type");
        }

        requireChatAccess(activityId, userEmail);
        User user = requireUserByEmail(userEmail);
        ActivityMessage message = messageRepository.findWithSenderAndActivityById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getActivity().getId().equals(activityId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to activity");
        }

        ActivityMessageReactionType actorReactionType = reactionType;

        ActivityMessageReaction existingReaction = reactionRepository
                .findByMessageIdAndUserId(messageId, user.getId())
                .orElse(null);

        if (existingReaction != null && existingReaction.getReactionType() == reactionType) {
            reactionRepository.delete(existingReaction);
            actorReactionType = null;
        } else if (existingReaction != null) {
            existingReaction.setReactionType(reactionType);
            existingReaction.setReactedAt(LocalDateTime.now());
            reactionRepository.save(existingReaction);
        } else {
            ActivityMessageReaction newReaction = new ActivityMessageReaction();
            newReaction.setMessage(message);
            newReaction.setUser(user);
            newReaction.setReactionType(reactionType);
            newReaction.setReactedAt(LocalDateTime.now());
            reactionRepository.save(newReaction);
        }

        Map<Long, Map<ActivityMessageReactionType, Long>> countsByMessage = buildReactionCountsByMessage(List.of(messageId));
        Map<Long, ActivityMessageReactionType> currentUserReactions = actorReactionType != null
            ? Map.of(messageId, actorReactionType)
            : Map.of();

        return new MessageReactionUpdateEvent(
                messageId,
                buildReactionSummaryForMessage(messageId, countsByMessage, currentUserReactions),
                user.getId(),
                actorReactionType != null ? actorReactionType.name() : null
        );
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

    @Transactional
    public void markAllMessagesSeen(Long activityId, String userEmail) {
        requireChatAccess(activityId, userEmail);
        User user = requireUserByEmail(userEmail);
        seenRepository.markAllMessagesSeenForActivity(activityId, user.getId(), LocalDateTime.now());
    }

    @Transactional
    public void markAllAnnouncementsSeen(Long activityId, String userEmail) {
        requireChatAccess(activityId, userEmail);
        User user = requireUserByEmail(userEmail);
        seenRepository.markAllAnnouncementsSeenForActivity(activityId, user.getId(), LocalDateTime.now());
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

    private ChatMessageResponse toMessageResponse(ActivityMessage message,
                                                  Map<Long, Map<ActivityMessageReactionType, Long>> reactionCountsByMessage,
                                                  Map<Long, ActivityMessageReactionType> currentUserReactionsByMessage) {
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
                seenCount,
                buildReactionSummaryForMessage(message.getId(), reactionCountsByMessage, currentUserReactionsByMessage),
                currentUserReactionsByMessage.get(message.getId()) != null
                        ? currentUserReactionsByMessage.get(message.getId()).name()
                        : null
        );
    }

    private Map<Long, Map<ActivityMessageReactionType, Long>> buildReactionCountsByMessage(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Map<ActivityMessageReactionType, Long>> result = new LinkedHashMap<>();
        for (Long messageId : messageIds) {
            result.put(messageId, new EnumMap<>(ActivityMessageReactionType.class));
        }

        List<ActivityMessageReactionRepository.MessageReactionCountView> rows = reactionRepository.countGroupedByMessageIds(messageIds);
        for (ActivityMessageReactionRepository.MessageReactionCountView row : rows) {
            Map<ActivityMessageReactionType, Long> messageCounts = result.get(row.getMessageId());
            if (messageCounts == null) {
                continue;
            }
            messageCounts.put(row.getReactionType(), row.getTotal());
        }

        return result;
    }

    private Map<Long, ActivityMessageReactionType> buildCurrentUserReactionsByMessage(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty() || userId == null) {
            return Map.of();
        }

        return reactionRepository.findByMessageIdInAndUserId(messageIds, userId)
                .stream()
                .collect(LinkedHashMap::new,
                        (map, reaction) -> map.put(reaction.getMessage().getId(), reaction.getReactionType()),
                        LinkedHashMap::putAll);
    }

    private List<MessageReactionSummaryResponse> buildReactionSummaryForMessage(
            Long messageId,
            Map<Long, Map<ActivityMessageReactionType, Long>> reactionCountsByMessage,
            Map<Long, ActivityMessageReactionType> currentUserReactionsByMessage
    ) {
        Map<ActivityMessageReactionType, Long> counts = reactionCountsByMessage.getOrDefault(
                messageId,
                Map.of()
        );
        ActivityMessageReactionType currentUserReaction = currentUserReactionsByMessage.get(messageId);

        return SUPPORTED_REACTIONS.stream()
                .map(type -> new MessageReactionSummaryResponse(
                        type.name(),
                        type.getEmoji(),
                        counts.getOrDefault(type, 0L),
                        currentUserReaction == type
                ))
                .toList();
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

    private String normalizeAnnouncementContent(String content) {
        if (content == null || content.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement content is required");
        }
        String normalized = content.trim();
        if (normalized.length() > MAX_ANNOUNCEMENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Announcement is too long");
        }
        return normalized;
    }

    private AnnouncementResponse toAnnouncementResponse(ActivityMessage message) {
        return new AnnouncementResponse(
                message.getId(),
                message.getActivity().getId(),
                message.getSender().getId(),
                message.getSender().getFullName(),
                message.getContent(),
                message.getCreatedAt()
        );
    }

    private void requireChatAccess(Long activityId, String userEmail) {
        if (!participationService.canAccessChat(activityId, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu ai acces la chat pentru aceasta activitate");
        }
    }

    private User requireActivityAuthor(Long activityId, String userEmail) {
        User user = requireUserByEmail(userEmail);
        Activity activity = requireActivity(activityId);
        if (activity.getCreator() == null || !activity.getCreator().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doar autorul activitatii poate modifica poll-urile");
        }
        return user;
    }

    private PollResponse getPollResponseById(Long activityId, Long pollId, Long currentUserId) {
        ActivityPoll poll = pollRepository.findByIdAndActivityId(pollId, activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Poll not found"));
        return buildPollResponses(List.of(poll), currentUserId).getFirst();
    }

    private List<PollResponse> buildPollResponses(List<ActivityPoll> polls, Long currentUserId) {
        List<Long> pollIds = polls.stream().map(ActivityPoll::getId).toList();
        List<ActivityPollOption> options = pollOptionRepository.findByPollIdInOrderByPollIdAscPositionAsc(pollIds);
        List<ActivityPollVote> votes = pollVoteRepository.findByPollIdInOrderByVotedAtAsc(pollIds);

        Map<Long, List<ActivityPollOption>> optionsByPoll = new LinkedHashMap<>();
        for (ActivityPoll poll : polls) {
            optionsByPoll.put(poll.getId(), new ArrayList<>());
        }
        for (ActivityPollOption option : options) {
            optionsByPoll.computeIfAbsent(option.getPoll().getId(), key -> new ArrayList<>()).add(option);
        }

        Map<Long, Map<Long, List<PollVoterResponse>>> votersByPollAndOption = new LinkedHashMap<>();
        Map<Long, Map<Long, Long>> countsByPollAndOption = new LinkedHashMap<>();
        Map<Long, Long> currentVoteByPoll = new LinkedHashMap<>();

        for (ActivityPoll poll : polls) {
            votersByPollAndOption.put(poll.getId(), new LinkedHashMap<>());
            countsByPollAndOption.put(poll.getId(), new LinkedHashMap<>());
        }

        for (ActivityPollVote vote : votes) {
            Long pollId = vote.getPoll().getId();
            Long optionId = vote.getOption().getId();

            countsByPollAndOption
                    .computeIfAbsent(pollId, key -> new LinkedHashMap<>())
                    .merge(optionId, 1L, Long::sum);

            votersByPollAndOption
                    .computeIfAbsent(pollId, key -> new LinkedHashMap<>())
                    .computeIfAbsent(optionId, key -> new ArrayList<>())
                    .add(new PollVoterResponse(
                            vote.getVoter().getId(),
                            vote.getVoter().getFullName(),
                            vote.getVotedAt()
                    ));

            if (currentUserId != null && currentUserId.equals(vote.getVoter().getId())) {
                currentVoteByPoll.put(pollId, optionId);
            }
        }

        List<PollResponse> response = new ArrayList<>();
        for (ActivityPoll poll : polls) {
            Long pollId = poll.getId();
            Long currentOptionId = currentVoteByPoll.get(pollId);
            List<PollOptionSummaryResponse> optionResponses = new ArrayList<>();

            List<ActivityPollOption> pollOptions = optionsByPoll.getOrDefault(pollId, List.of());
            for (ActivityPollOption option : pollOptions) {
                Long optionId = option.getId();
                long count = countsByPollAndOption.getOrDefault(pollId, Map.of()).getOrDefault(optionId, 0L);
                List<PollVoterResponse> voters = votersByPollAndOption
                        .getOrDefault(pollId, Map.of())
                        .getOrDefault(optionId, List.of());

                optionResponses.add(new PollOptionSummaryResponse(
                        optionId,
                        option.getText(),
                        count,
                        currentOptionId != null && currentOptionId.equals(optionId),
                        List.copyOf(voters)
                ));
            }

            response.add(new PollResponse(
                    pollId,
                    poll.getActivity().getId(),
                    poll.getCreator().getId(),
                    poll.getCreator().getFullName(),
                    poll.getQuestion(),
                    poll.getCreatedAt(),
                    currentOptionId,
                    List.copyOf(optionResponses)
            ));
        }

        return response;
    }

    private String normalizePollQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll question is required");
        }
        String normalized = question.trim();
        if (normalized.length() > MAX_POLL_QUESTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll question is too long");
        }
        return normalized;
    }

    private List<String> normalizePollOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least 2 options are required");
        }

        Set<String> seen = new HashSet<>();
        List<String> result = new ArrayList<>();
        for (String raw : options) {
            if (raw == null) {
                continue;
            }
            String normalized = raw.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            normalized = normalizePollOption(normalized);
            String key = normalized.toLowerCase();
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);
            result.add(normalized);
            if (result.size() == MAX_POLL_OPTIONS) {
                break;
            }
        }

        if (result.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least 2 different options are required");
        }

        return result;
    }

    private String normalizePollOption(String optionText) {
        if (optionText == null || optionText.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll option is required");
        }
        String normalized = optionText.trim();
        if (normalized.length() > MAX_POLL_OPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poll option is too long");
        }
        return normalized;
    }
}
