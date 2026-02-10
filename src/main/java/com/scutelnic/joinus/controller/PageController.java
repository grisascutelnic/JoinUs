package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.service.ActivityService;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    private final ActivityService activityService;
    private final UserRepository userRepository;

    public PageController(ActivityService activityService, UserRepository userRepository) {
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("recentActivities", activityService.getRecent(6));
        return "index";
    }

    @GetMapping("/profile")
    public String profile() {
        return "profile";
    }

    @GetMapping("/calendar")
    public String calendar() {
        return "calendar";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }

    @GetMapping("/map")
    public String map() {
        return "map";
    }

    @GetMapping("/forum")
    public String forum() {
        return "forum";
    }

    @GetMapping("/activities")
    public String activities(Model model) {
        model.addAttribute("activities", activityService.getAll());
        return "activities";
    }

    @GetMapping("/activities/{id}")
    public String activityDetail(@PathVariable Long id, Model model, Authentication authentication) {
        return activityService.getById(id)
                .map(activity -> {
                    model.addAttribute("activity", activity);
                    model.addAttribute("otherActivities", activityService.getRecent(6));
                    if (authentication != null && authentication.isAuthenticated()) {
                        userRepository.findByEmail(authentication.getName())
                                .ifPresent(user -> model.addAttribute("currentUserId", user.getId()));
                    }
                    return "activity-detail";
                })
                .orElse("redirect:/activities?missing");
    }

}
