package com.scutelnic.joinus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping({"/", "/index"})
    public String index() {
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
    public String activities() {
        return "activities";
    }

    @GetMapping("/notifications")
    public String notifications() {
        return "notifications";
    }
}
