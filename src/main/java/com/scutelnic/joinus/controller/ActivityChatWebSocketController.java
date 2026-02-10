package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.chat.ChatMessageRequest;
import com.scutelnic.joinus.dto.chat.ChatMessageResponse;
import com.scutelnic.joinus.dto.chat.DeliveredEventRequest;
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
}
