package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.chat.ChatMessageRequest;
import com.scutelnic.joinus.dto.chat.ChatMessageResponse;
import com.scutelnic.joinus.dto.chat.AnnouncementCreateRequest;
import com.scutelnic.joinus.dto.chat.AnnouncementResponse;
import com.scutelnic.joinus.dto.chat.DeliveredEventRequest;
import com.scutelnic.joinus.dto.chat.MessageReactionEventRequest;
import com.scutelnic.joinus.dto.chat.MessageReactionUpdateEvent;
import com.scutelnic.joinus.dto.chat.PollAddOptionRequest;
import com.scutelnic.joinus.dto.chat.PollCreateRequest;
import com.scutelnic.joinus.dto.chat.PollDeleteRequest;
import com.scutelnic.joinus.dto.chat.PollDeleteOptionRequest;
import com.scutelnic.joinus.dto.chat.PollEditRequest;
import com.scutelnic.joinus.dto.chat.PollEditOptionRequest;
import com.scutelnic.joinus.dto.chat.PollUpdateEvent;
import com.scutelnic.joinus.dto.chat.PollVoteRequest;
import com.scutelnic.joinus.dto.chat.SeenEventRequest;
import com.scutelnic.joinus.dto.chat.SeenUpdateEvent;
import com.scutelnic.joinus.service.ActivityChatService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

@Controller
public class ActivityChatWebSocketController {

    private final ActivityChatService activityChatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ActivityChatWebSocketController(ActivityChatService activityChatService,
                                           SimpMessagingTemplate messagingTemplate) {
        this.activityChatService = activityChatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/activities/{activityId}/chat")
    public void sendMessage(@DestinationVariable Long activityId,
                            ChatMessageRequest request,
                            Principal principal) {
        String email = requirePrincipal(principal);
        ChatMessageResponse created = activityChatService.sendMessage(activityId, email, request.content());
        messagingTemplate.convertAndSend(topicForActivity(activityId), created);
    }

    @MessageMapping("/activities/{activityId}/announcements/create")
    public void createAnnouncement(@DestinationVariable Long activityId,
                                   AnnouncementCreateRequest request,
                                   Principal principal) {
        String email = requirePrincipal(principal);
        AnnouncementResponse created = activityChatService.createAnnouncement(activityId, email, request.content());
        messagingTemplate.convertAndSend(announcementsTopicForActivity(activityId), created);
    }

    @MessageMapping("/activities/{activityId}/seen")
    public void markSeen(@DestinationVariable Long activityId,
                         SeenEventRequest request,
                         Principal principal) {
        String email = requirePrincipal(principal);
        SeenUpdateEvent update = activityChatService.markSeen(activityId, request.messageId(), email);
        messagingTemplate.convertAndSend(statusTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/delivered")
    public void markDelivered(@DestinationVariable Long activityId,
                              DeliveredEventRequest request,
                              Principal principal) {
        String email = requirePrincipal(principal);
        SeenUpdateEvent update = activityChatService.markDelivered(activityId, request.messageId(), email);
        messagingTemplate.convertAndSend(statusTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/reactions")
    public void toggleReaction(@DestinationVariable Long activityId,
                               MessageReactionEventRequest request,
                               Principal principal) {
        String email = requirePrincipal(principal);
        MessageReactionUpdateEvent update = activityChatService.toggleReaction(
                activityId,
                request.messageId(),
                request.reactionType(),
                email
        );
        messagingTemplate.convertAndSend(reactionsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/create")
    public void createPoll(@DestinationVariable Long activityId,
                           PollCreateRequest request,
                           Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.createPoll(activityId, email, request.question(), request.options());
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/vote")
    public void votePoll(@DestinationVariable Long activityId,
                         PollVoteRequest request,
                         Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.votePoll(activityId, request.pollId(), request.optionId(), email);
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/edit")
    public void editPoll(@DestinationVariable Long activityId,
                         PollEditRequest request,
                         Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.editPoll(activityId, request.pollId(), request.question(), email);
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/delete")
    public void deletePoll(@DestinationVariable Long activityId,
                           PollDeleteRequest request,
                           Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.deletePoll(activityId, request.pollId(), email);
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/add-option")
    public void addPollOption(@DestinationVariable Long activityId,
                              PollAddOptionRequest request,
                              Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.addPollOption(activityId, request.pollId(), request.optionText(), email);
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/edit-option")
    public void editPollOption(@DestinationVariable Long activityId,
                               PollEditOptionRequest request,
                               Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.editPollOption(
                activityId,
                request.pollId(),
                request.optionId(),
                request.optionText(),
                email
        );
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    @MessageMapping("/activities/{activityId}/polls/delete-option")
    public void deletePollOption(@DestinationVariable Long activityId,
                                 PollDeleteOptionRequest request,
                                 Principal principal) {
        String email = requirePrincipal(principal);
        PollUpdateEvent update = activityChatService.deletePollOption(
                activityId,
                request.pollId(),
                request.optionId(),
                email
        );
        messagingTemplate.convertAndSend(pollsTopicForActivity(activityId), update);
    }

    private String requirePrincipal(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal.getName();
    }

    private String topicForActivity(Long activityId) {
        return "/topic/activities/" + activityId;
    }

    private String statusTopicForActivity(Long activityId) {
        return "/topic/activities/" + activityId + "/status";
    }

    private String reactionsTopicForActivity(Long activityId) {
        return "/topic/activities/" + activityId + "/reactions";
    }

    private String pollsTopicForActivity(Long activityId) {
        return "/topic/activities/" + activityId + "/polls";
    }

    private String announcementsTopicForActivity(Long activityId) {
        return "/topic/activities/" + activityId + "/announcements";
    }
}
