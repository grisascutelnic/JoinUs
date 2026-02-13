package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.service.UserService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final UserService userService;

    public GlobalModelAttributes(UserService userService) {
        this.userService = userService;
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

    private java.util.Optional<User> resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return java.util.Optional.empty();
        }
        return userService.findByEmail(authentication.getName());
    }
}