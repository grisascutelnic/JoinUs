package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.service.ActivityMemoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ActivityMemoryController {

    private final ActivityMemoryService activityMemoryService;

    public ActivityMemoryController(ActivityMemoryService activityMemoryService) {
        this.activityMemoryService = activityMemoryService;
    }

    @PostMapping("/activities/{id}/memories")
    public String addMemory(@PathVariable Long id,
                            @RequestParam(value = "content", required = false) String content,
                            @RequestParam(value = "memoryFiles", required = false) List<MultipartFile> memoryFiles,
                            @RequestParam(value = "photoCaptions", required = false) List<String> photoCaptions,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }

        try {
            activityMemoryService.addEntry(id, authentication.getName(), content, memoryFiles, photoCaptions);
            redirectAttributes.addFlashAttribute("memoryMessage", "Amintirea a fost adaugata.");
            redirectAttributes.addFlashAttribute("memoryMessageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("memoryMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("memoryMessageType", "danger");
        }

        return "redirect:/activities/" + id + "?tab=memories";
    }

    @PostMapping("/activities/{id}/memories/publish")
    public String publishBook(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }

        try {
            activityMemoryService.publishBook(id, authentication.getName());
            redirectAttributes.addFlashAttribute("memoryMessage", "Cartea publica a fost actualizata.");
            redirectAttributes.addFlashAttribute("memoryMessageType", "success");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("memoryMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("memoryMessageType", "danger");
        }

        return "redirect:/activities/" + id + "?tab=memories";
    }
}
