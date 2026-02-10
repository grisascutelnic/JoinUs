package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.chat.ChatMessageResponse;
import com.scutelnic.joinus.dto.chat.MessageSeenSummaryResponse;
import com.scutelnic.joinus.dto.chat.SeenUserResponse;
import com.scutelnic.joinus.service.ActivityChatService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActivityChatApiController {

    private final ActivityChatService activityChatService;

    public ActivityChatApiController(ActivityChatService activityChatService) {
        this.activityChatService = activityChatService;
    }

    @GetMapping("/activities/{activityId}/messages")
    public List<ChatMessageResponse> getMessages(@PathVariable Long activityId,
                                                 @RequestParam(defaultValue = "50") int limit,
                                                 Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getRecentMessages(activityId, limit);
    }

    @GetMapping("/messages/{messageId}/seen")
    public List<SeenUserResponse> getSeenUsers(@PathVariable Long messageId,
                                               Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getSeenUsers(messageId);
    }

    @GetMapping("/activities/{activityId}/messages/seen")
    public List<MessageSeenSummaryResponse> getSeenUsersForMessages(@PathVariable Long activityId,
                                                                    @RequestParam List<Long> messageIds,
                                                                    Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getSeenUsersForMessages(activityId, messageIds);
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
