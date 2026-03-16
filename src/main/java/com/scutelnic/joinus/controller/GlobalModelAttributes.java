package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.service.ActivityUnreadService;
import com.scutelnic.joinus.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;
    private final ActivityUnreadService activityUnreadService;

    public GlobalModelAttributes(UserService userService,
                                 ActivityUnreadService activityUnreadService) {
        this.userService = userService;
        this.activityUnreadService = activityUnreadService;
    }

    @ModelAttribute("currentUserDisplayName")
    public String currentUserDisplayName(Authentication authentication) {
        return resolveAuthenticatedUser(authentication)
                .map(User::getFullName)
                .filter(name -> !name.isBlank())
                .orElse(null);
    }

    @ModelAttribute("currentUserAvatarUrl")
    public String currentUserAvatarUrl(Authentication authentication) {
        return resolveAuthenticatedUser(authentication)
                .map(User::getAvatarUrl)
                .filter(url -> !url.isBlank())
                .orElse(null);
    }

    @ModelAttribute("unreadChatGroupsCount")
    public long unreadChatGroupsCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return 0;
        }
        return activityUnreadService.countUnreadActivities(authentication.getName());
    }

    @ModelAttribute("activityUnreadCounts")
    public java.util.Map<Long, Long> activityUnreadCounts(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return java.util.Map.of();
        }
        return activityUnreadService.getUnreadCountsByActivityForUser(authentication.getName());
    }

    private java.util.Optional<User> resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return java.util.Optional.empty();
        }
        return userService.findByEmail(authentication.getName());
    }
}