package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.AdminRequest;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.UserRepository;
import com.scutelnic.joinus.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String TARGET_USER = "USER";
    private static final String TARGET_ALL = "ALL";

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public AdminController(NotificationService notificationService,
                           UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String page(Model model) {
        if (!model.containsAttribute("adminRequest")) {
            AdminRequest request = new AdminRequest();
            request.setTargetType(TARGET_USER);
            model.addAttribute("adminRequest", request);
        }
        model.addAttribute("users", getUsers());
        return "admin";
    }

    @PostMapping
    public String send(@Valid @ModelAttribute("adminRequest") AdminRequest request,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("users", getUsers());
            return "admin";
        }

        String targetType = normalizeTarget(request.getTargetType());
        String title = normalizeRequired(request.getTitle());
        String message = normalizeRequired(request.getMessage());
        String link = normalizeNullable(request.getLink());

        if (TARGET_ALL.equals(targetType)) {
            int sentCount = notificationService.sendAdminNotificationToAll(title, message, link);
            model.addAttribute("successMessage", "Notificarea a fost trimisa catre " + sentCount + " utilizatori.");
        } else if (TARGET_USER.equals(targetType)) {
            if (request.getRecipientUserId() == null) {
                model.addAttribute("users", getUsers());
                model.addAttribute("errorMessage", "Selecteaza un utilizator.");
                return "admin";
            }
            notificationService.sendAdminNotificationToUser(request.getRecipientUserId(), title, message, link);
            model.addAttribute("successMessage", "Notificarea a fost trimisa utilizatorului selectat.");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid notification target");
        }

        model.addAttribute("users", getUsers());
        AdminRequest reset = new AdminRequest();
        reset.setTargetType(TARGET_USER);
        model.addAttribute("adminRequest", reset);
        return "admin";
    }

    private List<User> getUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName", "email"));
    }

    private static String normalizeTarget(String target) {
        return target == null ? "" : target.trim().toUpperCase();
    }

    private static String normalizeRequired(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
