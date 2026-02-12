package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.ActivityDto;
import com.scutelnic.joinus.service.ActivityService;
import com.scutelnic.joinus.service.CloudinaryService;
import com.scutelnic.joinus.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final UserRepository userRepository;

    public ActivityController(ActivityService activityService, CloudinaryService cloudinaryService, UserRepository userRepository) {
        this.activityService = activityService;
        this.cloudinaryService = cloudinaryService;
        this.userRepository = userRepository;
    }

    @GetMapping("/activities/new")
    public String createActivityForm(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }
        if (!hasBirthDate(authentication.getName())) {
            return "redirect:/profile?birthDateRequired&fromActivity";
        }

        ActivityDto form = new ActivityDto();
        form.setDate(LocalDate.now());
        form.setTime(LocalTime.of(18, 0));
        form.setCapacity(10);
        form.setCategory("Comunitate");
        prepareFormModel(model, form, "/activities", "Formular activitate", "Completeaza detaliile si alege o imagine.", "Creeaza activitate", false);
        return "create-activity";
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
            return "redirect:/profile?birthDateRequired&fromActivity";
        }

        var creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, form, "/activities", "Formular activitate", "Completeaza detaliile si alege o imagine.", "Creeaza activitate", false);
            return "create-activity";
        }

        if (imageFile != null && !imageFile.isEmpty()
                && (form.getImageUrl() == null || form.getImageUrl().isBlank())) {
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
                imageUrl,
                creator
        );

        return "redirect:/activities?created";
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
            if (DEFAULT_IMAGES.contains(existingImage)) {
                form.setImageChoice(existingImage);
            } else {
                form.setImageUrl(existingImage);
            }
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

        if (imageFile != null && !imageFile.isEmpty()
                && (form.getImageUrl() == null || form.getImageUrl().isBlank())) {
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
        activity.setImageUrl(resolveImageUrlForEdit(form.getImageUrl(), form.getImageChoice(), activity.getImageUrl()));
        activityService.create(activity);

        return "redirect:/activities/" + id + "?updated";
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
        model.addAttribute("imageOptions", DEFAULT_IMAGES);
        model.addAttribute("cloudinaryCloudName", cloudinaryService.getCloudName());
        model.addAttribute("cloudinaryUploadPreset", cloudinaryService.getUploadPreset());
    }

    private boolean isOwner(String creatorEmail, String authenticatedEmail) {
        return creatorEmail != null
                && authenticatedEmail != null
                && creatorEmail.equalsIgnoreCase(authenticatedEmail);
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

    private String resolveImageUrlForEdit(String imageUrl, String imageChoice, String existingImageUrl) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        if (imageChoice != null && !imageChoice.isBlank()) {
            return imageChoice;
        }
        if (existingImageUrl != null && !existingImageUrl.isBlank()) {
            return existingImageUrl;
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

    private boolean hasBirthDate(String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getBirthDate() != null)
                .orElse(false);
    }

}
