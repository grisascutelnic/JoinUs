package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.ActivityDto;
import com.scutelnic.joinus.service.ActivityService;
import com.scutelnic.joinus.service.CloudinaryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class ActivityController {

    private static final List<String> DEFAULT_IMAGES = List.of(
            "/images/anna-rosar-ZxFyVBHMK-c-unsplash.jpg",
            "/images/frederik-rosar-NDSZcCfnsbY-unsplash.jpg",
            "/images/anna-rosar-ew-olGvgCCs-unsplash.jpg",
            "/images/girl-taking-selfie-with-friends-golf-field.jpg",
            "/images/professional-golf-player.jpg"
    );

    private final ActivityService activityService;
    private final CloudinaryService cloudinaryService;

    public ActivityController(ActivityService activityService, CloudinaryService cloudinaryService) {
        this.activityService = activityService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/activities/new")
    public String createActivityForm(Model model) {
        ActivityDto form = new ActivityDto();
        form.setDate(LocalDate.now());
        form.setTime(LocalTime.of(18, 0));
        form.setCapacity(10);
        form.setCategory("Comunitate");
        model.addAttribute("activityForm", form);
        model.addAttribute("imageOptions", DEFAULT_IMAGES);
        model.addAttribute("cloudinaryCloudName", cloudinaryService.getCloudName());
        model.addAttribute("cloudinaryUploadPreset", cloudinaryService.getUploadPreset());
        return "create-activity";
    }

    @PostMapping("/activities")
    public String createActivity(
            @Valid @ModelAttribute("activityForm") ActivityDto form,
            BindingResult bindingResult,
            Model model,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("imageOptions", DEFAULT_IMAGES);
            model.addAttribute("cloudinaryCloudName", cloudinaryService.getCloudName());
            model.addAttribute("cloudinaryUploadPreset", cloudinaryService.getUploadPreset());
            return "create-activity";
        }

        if (imageFile != null && !imageFile.isEmpty()
                && (form.getImageUrl() == null || form.getImageUrl().isBlank())) {
            if (!cloudinaryService.isConfigured()) {
                model.addAttribute("imageError", "Încărcarea nu este configurată. Completează Cloudinary în .env.");
                model.addAttribute("imageOptions", DEFAULT_IMAGES);
                model.addAttribute("cloudinaryCloudName", cloudinaryService.getCloudName());
                model.addAttribute("cloudinaryUploadPreset", cloudinaryService.getUploadPreset());
                return "create-activity";
            }
            try {
                String uploadedUrl = cloudinaryService.uploadImage(imageFile);
                if (uploadedUrl != null && !uploadedUrl.isBlank()) {
                    form.setImageUrl(uploadedUrl);
                    form.setImageChoice(null);
                }
            } catch (Exception ignored) {
                // fallback to selected image/default
            }
        }

        String imageUrl = resolveImageUrl(form.getImageUrl(), form.getImageChoice());
        activityService.create(
                form.getTitle(),
                form.getDescription(),
                form.getDate(),
                form.getTime(),
                form.getLocation(),
                form.getAddress(),
                form.getCapacity(),
                normalizeCategory(form.getCategory()),
                normalizeTags(form.getTags()),
                imageUrl
        );

        return "redirect:/activities?created";
    }

    private String resolveImageUrl(String imageUrl, String imageChoice) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        if (imageChoice != null && !imageChoice.isBlank()) {
            return imageChoice;
        }
        return DEFAULT_IMAGES.get(0);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "Comunitate";
        }
        return category.trim();
    }

    private String normalizeTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return "";
        }
        String[] parts = tags.split(",");
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(trimmed);
            count++;
            if (count == 3) {
                break;
            }
        }
        return builder.toString();
    }

}
