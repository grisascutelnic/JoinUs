package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.chat.ChatMessageResponse;
import com.scutelnic.joinus.dto.chat.ChatUnreadSummaryResponse;
import com.scutelnic.joinus.dto.chat.MessageSeenSummaryResponse;
import com.scutelnic.joinus.dto.chat.PollResponse;
import com.scutelnic.joinus.dto.chat.SeenUserResponse;
import com.scutelnic.joinus.service.ActivityChatService;
import com.scutelnic.joinus.service.ActivityUnreadService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActivityChatApiController {

    private final ActivityChatService activityChatService;
    private final ActivityUnreadService activityUnreadService;

    public ActivityChatApiController(ActivityChatService activityChatService,
                                     ActivityUnreadService activityUnreadService) {
        this.activityChatService = activityChatService;
        this.activityUnreadService = activityUnreadService;
    }

    @GetMapping("/activities/{activityId}/messages")
    public List<ChatMessageResponse> getMessages(@PathVariable Long activityId,
                                                 @RequestParam(defaultValue = "50") int limit,
                                                 Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getRecentMessages(activityId, limit, authentication.getName());
    }

    @GetMapping("/messages/{messageId}/seen")
    public List<SeenUserResponse> getSeenUsers(@PathVariable Long messageId,
                                               Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getSeenUsers(messageId, authentication.getName());
    }

    @GetMapping("/activities/{activityId}/messages/seen")
    public List<MessageSeenSummaryResponse> getSeenUsersForMessages(@PathVariable Long activityId,
                                                                    @RequestParam List<Long> messageIds,
                                                                    Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getSeenUsersForMessages(activityId, messageIds, authentication.getName());
    }

    @GetMapping("/activities/{activityId}/polls")
    public List<PollResponse> getPolls(@PathVariable Long activityId,
                                       Authentication authentication) {
        requireAuthenticated(authentication);
        return activityChatService.getPolls(activityId, authentication.getName());
    }

    @PostMapping("/activities/{activityId}/messages/mark-all-seen")
    public void markAllMessagesSeen(@PathVariable Long activityId,
                                    Authentication authentication) {
        requireAuthenticated(authentication);
        activityChatService.markAllMessagesSeen(activityId, authentication.getName());
    }

    @GetMapping("/chat/unread-summary")
    public ChatUnreadSummaryResponse getUnreadSummary(Authentication authentication) {
        requireAuthenticated(authentication);
        String email = authentication.getName();
        var unreadCounts = activityUnreadService.getUnreadCountsByActivityForUser(email);
        var latestUnreadByActivity = activityUnreadService
                .getLatestUnreadMessageByActivityForUser(email, unreadCounts.keySet());
        return new ChatUnreadSummaryResponse(
                unreadCounts.size(),
                unreadCounts,
                latestUnreadByActivity
        );
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
