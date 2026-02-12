package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.entity.ActivityParticipation;
import com.scutelnic.joinus.entity.ParticipationStatus;
import com.scutelnic.joinus.service.ActivityParticipationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ActivityParticipationController {

    private final ActivityParticipationService participationService;

    public ActivityParticipationController(ActivityParticipationService participationService) {
        this.participationService = participationService;
    }

    @PostMapping("/activities/{activityId}/participation-request")
    public String requestParticipation(@PathVariable Long activityId,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            ActivityParticipation participation = participationService.requestParticipation(activityId, authentication.getName());
            if (participation.getStatus() == ParticipationStatus.APPROVED) {
                redirectAttributes.addFlashAttribute("participationMessage", "Esti deja participant la aceasta activitate.");
                redirectAttributes.addFlashAttribute("participationMessageType", "info");
            } else {
                redirectAttributes.addFlashAttribute("participationMessage", "Cerere trimisa. Status: in asteptare.");
                redirectAttributes.addFlashAttribute("participationMessageType", "warning");
            }
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("participationMessage", mapMessage(ex.getReason(), "Nu am putut trimite cererea."));
            redirectAttributes.addFlashAttribute("participationMessageType", "danger");
        }

        return "redirect:/activities/" + activityId;
    }

    @PostMapping("/activities/{activityId}/participation-requests/{requestId}/approve")
    public String approveRequest(@PathVariable Long activityId,
                                 @PathVariable Long requestId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            participationService.approveRequest(activityId, requestId, authentication.getName());
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("organizerMessage", mapMessage(ex.getReason(), "Nu am putut accepta cererea."));
            redirectAttributes.addFlashAttribute("organizerMessageType", "danger");
        }

        return "redirect:/activities/" + activityId;
    }

    @PostMapping("/activities/{activityId}/participation-requests/{requestId}/reject")
    public String rejectRequest(@PathVariable Long activityId,
                                @PathVariable Long requestId,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            participationService.rejectRequest(activityId, requestId, authentication.getName());
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("organizerMessage", mapMessage(ex.getReason(), "Nu am putut respinge cererea."));
            redirectAttributes.addFlashAttribute("organizerMessageType", "danger");
        }

        return "redirect:/activities/" + activityId;
    }

    @PostMapping("/activities/{activityId}/participation-requests/{requestId}/exclude")
    public String excludeParticipant(@PathVariable Long activityId,
                                     @PathVariable Long requestId,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        try {
            participationService.excludeParticipant(activityId, requestId, authentication.getName());
        } catch (ResponseStatusException ex) {
            redirectAttributes.addFlashAttribute("organizerMessage", mapMessage(ex.getReason(), "Nu am putut exclude participantul."));
            redirectAttributes.addFlashAttribute("organizerMessageType", "danger");
        }

        return "redirect:/activities/" + activityId;
    }

    private String mapMessage(String reason, String fallback) {
        if (reason == null || reason.isBlank()) {
            return fallback;
        }
        return reason;
    }
}
