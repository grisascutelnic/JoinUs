package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.ActivityDto;
import com.scutelnic.joinus.service.ActivityService;
import com.scutelnic.joinus.service.ActivityChatService;
import com.scutelnic.joinus.service.CloudinaryService;
import com.scutelnic.joinus.service.PollinationsImageService;
import com.scutelnic.joinus.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Controller
public class ActivityController {

    private final ActivityService activityService;
    private final CloudinaryService cloudinaryService;
    private final PollinationsImageService pollinationsImageService;
    private final UserRepository userRepository;
    private final ActivityChatService activityChatService;

    public ActivityController(ActivityService activityService,
                              CloudinaryService cloudinaryService,
                              PollinationsImageService pollinationsImageService,
                              UserRepository userRepository,
                              ActivityChatService activityChatService) {
        this.activityService = activityService;
        this.cloudinaryService = cloudinaryService;
        this.pollinationsImageService = pollinationsImageService;
        this.userRepository = userRepository;
        this.activityChatService = activityChatService;
    }

    @GetMapping("/activities/new")
    public String createActivityForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }
        if (!hasBirthDate(authentication.getName())) {
            return "redirect:/profile/edit?birthDateRequired&fromActivity";
        }

        return "redirect:/activities?openCreate=true";
    }

    @PostMapping("/activities")
    public String createActivity(
            @Valid @ModelAttribute("activityForm") ActivityDto form,
            BindingResult bindingResult,
            Model model,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        if (!hasBirthDate(email)) {
            return "redirect:/profile/edit?birthDateRequired&fromActivity";
        }

        var creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, form, "/activities", "Formular activitate", "Completeaza detaliile si alege o imagine.", "Creeaza activitate", false);
            return "create-activity";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (!cloudinaryService.isConfigured()) {
                model.addAttribute("imageError", "Încărcarea nu este configurată. Completează Cloudinary în .env.");
                prepareFormModel(model, form, "/activities", "Formular activitate", "Completeaza detaliile si alege o imagine.", "Creeaza activitate", false);
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

        String imageUrl = resolveImageUrlForCreate(form);
        var createdActivity = activityService.create(
                form.getTitle(),
                form.getDescription(),
                form.getDate(),
                form.getTime(),
                form.getLocation(),
                form.getAddress(),
                form.getCapacity(),
                normalizeCategory(form.getCategory()),
                normalizeTags(form.getTags()),
                imageUrl,
                creator
        );
            activityChatService.ensureDefaultWelcomeAnnouncement(createdActivity.getId());

        return "redirect:/activities?created";
    }

    @PostMapping("/api/activities/generate-image")
    @ResponseBody
    public ResponseEntity<Map<String, String>> generateActivityImage(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String imageUrl = toStableGeneratedImageUrl(pollinationsImageService.buildImageUrl(title, description));
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @GetMapping("/activities/{id}/edit")
    public String editActivityForm(
            @PathVariable Long id,
            Authentication authentication,
            Model model
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        var activityOpt = activityService.getById(id);
        if (activityOpt.isEmpty()) {
            return "redirect:/activities?missing";
        }

        var activity = activityOpt.get();
        if (!isOwner(activity.getCreator().getEmail(), authentication.getName())) {
            return "redirect:/activities/" + id + "?forbidden";
        }

        ActivityDto form = new ActivityDto();
        form.setTitle(activity.getTitle());
        form.setDescription(activity.getDescription());
        form.setDate(activity.getDate());
        form.setTime(activity.getTime());
        form.setLocation(activity.getLocation());
        form.setAddress(activity.getAddress());
        form.setCapacity(activity.getCapacity());
        form.setCategory(activity.getCategory());
        form.setTags(activity.getTags());

        String existingImage = activity.getImageUrl();
        if (existingImage != null && !existingImage.isBlank()) {
            form.setImageUrl(existingImage);
        }

        prepareFormModel(
                model,
                form,
                "/activities/" + id + "/edit",
                "Editare activitate",
                "Modifica detaliile activitatii fara sa completezi totul din nou.",
                "Salveaza modificarile",
                true
        );
        return "create-activity";
    }

    @PostMapping("/activities/{id}/edit")
    public String editActivity(
            @PathVariable Long id,
            @Valid @ModelAttribute("activityForm") ActivityDto form,
            BindingResult bindingResult,
            Model model,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        var activityOpt = activityService.getById(id);
        if (activityOpt.isEmpty()) {
            return "redirect:/activities?missing";
        }

        var activity = activityOpt.get();
        if (!isOwner(activity.getCreator().getEmail(), authentication.getName())) {
            return "redirect:/activities/" + id + "?forbidden";
        }

        if (bindingResult.hasErrors()) {
            prepareFormModel(
                    model,
                    form,
                    "/activities/" + id + "/edit",
                    "Editare activitate",
                    "Modifica detaliile activitatii fara sa completezi totul din nou.",
                    "Salveaza modificarile",
                    true
            );
            return "create-activity";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (!cloudinaryService.isConfigured()) {
                model.addAttribute("imageError", "Încărcarea nu este configurată. Completează Cloudinary în .env.");
                prepareFormModel(
                        model,
                        form,
                        "/activities/" + id + "/edit",
                        "Editare activitate",
                        "Modifica detaliile activitatii fara sa completezi totul din nou.",
                        "Salveaza modificarile",
                        true
                );
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

        activity.setTitle(form.getTitle());
        activity.setDescription(form.getDescription());
        activity.setDate(form.getDate());
        activity.setTime(form.getTime());
        activity.setLocation(form.getLocation());
        activity.setAddress(form.getAddress());
        activity.setCapacity(form.getCapacity());
        activity.setCategory(normalizeCategory(form.getCategory()));
        activity.setTags(normalizeTags(form.getTags()));
        activity.setImageUrl(resolveImageUrlForEdit(form, activity.getImageUrl()));
        activityService.create(activity);

        return "redirect:/activities/" + id + "?updated";
    }

    @PostMapping("/activities/{id}/delete")
    public String deleteActivity(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        var activityOpt = activityService.getById(id);
        if (activityOpt.isEmpty()) {
            return "redirect:/activities?missing";
        }

        var activity = activityOpt.get();
        if (!isOwner(activity.getCreator().getEmail(), authentication.getName())) {
            return "redirect:/activities/" + id + "?forbidden";
        }

        activityService.deleteActivityWithRelations(id);
        return "redirect:/activities?deleted";
    }

    private void prepareFormModel(Model model,
                                  ActivityDto form,
                                  String formAction,
                                  String formTitle,
                                  String formSubtitle,
                                  String submitLabel,
                                  boolean editMode) {
        model.addAttribute("activityForm", form);
        model.addAttribute("formAction", formAction);
        model.addAttribute("formTitle", formTitle);
        model.addAttribute("formSubtitle", formSubtitle);
        model.addAttribute("submitLabel", submitLabel);
        model.addAttribute("editMode", editMode);
        model.addAttribute("cloudinaryCloudName", cloudinaryService.getCloudName());
        model.addAttribute("cloudinaryUploadPreset", cloudinaryService.getUploadPreset());
    }

    private boolean isOwner(String creatorEmail, String authenticatedEmail) {
        return creatorEmail != null
                && authenticatedEmail != null
                && creatorEmail.equalsIgnoreCase(authenticatedEmail);
    }

    private String resolveImageUrlForCreate(ActivityDto form) {
        if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            return form.getImageUrl();
        }
        if (form.getImageChoice() != null && !form.getImageChoice().isBlank()) {
            return toStableGeneratedImageUrl(form.getImageChoice());
        }
        return toStableGeneratedImageUrl(pollinationsImageService.buildImageUrl(
                form.getTitle(),
            form.getDescription()
        ));
    }

    private String resolveImageUrlForEdit(ActivityDto form, String existingImageUrl) {
        if (form.getImageUrl() != null && !form.getImageUrl().isBlank()) {
            return form.getImageUrl();
        }
        if (form.getImageChoice() != null && !form.getImageChoice().isBlank()) {
            return toStableGeneratedImageUrl(form.getImageChoice());
        }
        if (existingImageUrl != null && !existingImageUrl.isBlank()) {
            return existingImageUrl;
        }
        return toStableGeneratedImageUrl(pollinationsImageService.buildImageUrl(
                form.getTitle(),
            form.getDescription()
        ));
    }

    private String toStableGeneratedImageUrl(String imageUrl) {
        String normalizedUrl = pollinationsImageService.normalizeLegacyUrl(imageUrl);
        if (!pollinationsImageService.isPollinationsUrl(normalizedUrl)) {
            return normalizedUrl;
        }

        if (!cloudinaryService.isConfigured()) {
            return normalizedUrl;
        }

        try {
            String uploaded = cloudinaryService.uploadImageFromUrl(normalizedUrl);
            if (uploaded != null && !uploaded.isBlank()) {
                return uploaded;
            }
        } catch (Exception ignored) {
            // If Cloudinary cannot ingest the generated image, keep remote URL fallback.
        }
        return normalizedUrl;
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

    private boolean hasBirthDate(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getBirthDate() != null)
                .orElse(false);
    }

}
