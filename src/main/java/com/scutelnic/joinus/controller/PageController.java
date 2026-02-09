package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.service.ActivityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    private final ActivityService activityService;

    public PageController(ActivityService activityService) {
        this.activityService = activityService;
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

}
