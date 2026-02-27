package com.scutelnic.joinus.controller;

import com.scutelnic.joinus.dto.ProfileUpdateRequest;
import com.scutelnic.joinus.dto.UserReviewRequest;
import com.scutelnic.joinus.dto.UserReviewSummary;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.entity.ParticipationStatus;
import com.scutelnic.joinus.service.ActivityService;
import com.scutelnic.joinus.service.ActivityParticipationService;
import com.scutelnic.joinus.service.CloudinaryService;
import com.scutelnic.joinus.service.UserReviewService;
import com.scutelnic.joinus.service.UserService;
import com.scutelnic.joinus.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PageController {

    private final ActivityService activityService;
    private final ActivityParticipationService participationService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final UserReviewService userReviewService;
    private final CloudinaryService cloudinaryService;

    public PageController(ActivityService activityService,
                          ActivityParticipationService participationService,
                          UserRepository userRepository,
                          UserService userService,
                          UserReviewService userReviewService,
                          CloudinaryService cloudinaryService) {
        this.activityService = activityService;
        this.participationService = participationService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.userReviewService = userReviewService;
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        model.addAttribute("recentActivities", activityService.getRecent(6));
        return "index";
    }

    @GetMapping("/profile")
    public String profile(Model model,
                          Authentication authentication,
                          @RequestParam(required = false) String completeProfile,
                          @RequestParam(required = false) String birthDateRequired,
                          @RequestParam(required = false) String errorBirthDateRequired) {
        if (completeProfile != null || birthDateRequired != null || errorBirthDateRequired != null) {
            StringBuilder redirect = new StringBuilder("redirect:/profile/edit");
            if (birthDateRequired != null || errorBirthDateRequired != null || completeProfile != null) {
                redirect.append("?");
                boolean hasPrev = false;
                if (completeProfile != null) {
                    redirect.append("completeProfile");
                    hasPrev = true;
                }
                if (birthDateRequired != null) {
                    if (hasPrev) {
                        redirect.append("&");
                    }
                    redirect.append("birthDateRequired");
                    hasPrev = true;
                }
                if (errorBirthDateRequired != null) {
                    if (hasPrev) {
                        redirect.append("&");
                    }
                    redirect.append("errorBirthDateRequired");
                }
            }
            return redirect.toString();
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu exista."));
        populateOwnProfileModel(model, user, false);
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model,
                              Authentication authentication,
                              @RequestParam(required = false) String completeProfile,
                              @RequestParam(required = false) String birthDateRequired,
                              @RequestParam(required = false) String errorBirthDateRequired) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }

        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu exista."));
        boolean mustCompleteBirthDate = user.getBirthDate() == null
                && (completeProfile != null || birthDateRequired != null || errorBirthDateRequired != null);

        populateOwnProfileModel(model, user, mustCompleteBirthDate);
        return "profile-edit";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileUpdateRequest profileForm,
                                BindingResult bindingResult,
                                Authentication authentication,
                                @RequestParam(defaultValue = "false") boolean requireBirthDate,
                                @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/?login";
        }

        if (requireBirthDate && profileForm.getBirthDate() == null) {
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile/edit?birthDateRequired&errorBirthDateRequired";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileForm", bindingResult);
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            if (requireBirthDate) {
                return "redirect:/profile/edit?birthDateRequired&errorBirthDateRequired";
            }
            return "redirect:/profile/edit?error";
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (!cloudinaryService.isConfigured()) {
                redirectAttributes.addFlashAttribute("profileForm", profileForm);
                redirectAttributes.addFlashAttribute("profileImageError", "Upload-ul avatarului nu este configurat.");
                return "redirect:/profile/edit?error";
            }
            try {
                String uploadedUrl = cloudinaryService.uploadImage(imageFile);
                if (uploadedUrl != null && !uploadedUrl.isBlank()) {
                    profileForm.setAvatarUrl(uploadedUrl);
                }
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("profileForm", profileForm);
                redirectAttributes.addFlashAttribute("profileImageError", "Nu am putut incarca avatarul. Incearca din nou.");
                return "redirect:/profile/edit?error";
            }
        }

        try {
            userService.updateProfile(authentication.getName(), profileForm);
            return "redirect:/profile/edit?updated";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("profileForm", profileForm);
            return "redirect:/profile/edit?error";
        }
    }

    @GetMapping("/calendar")
    public String calendar(Model model, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            model.addAttribute("calendarActivities", List.of());
            model.addAttribute("isAuthenticatedUser", false);
            return "calendar";
        }

        User user = userService.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            model.addAttribute("calendarActivities", List.of());
            model.addAttribute("isAuthenticatedUser", true);
            return "calendar";
        }

        List<com.scutelnic.joinus.entity.Activity> authoredActivities = activityService.getByCreator(user.getId());
        List<com.scutelnic.joinus.entity.Activity> approvedActivities = participationService
            .getApprovedActivitiesForUser(authentication.getName());

        List<com.scutelnic.joinus.entity.Activity> combined = new ArrayList<>(authoredActivities.size() + approvedActivities.size());
        combined.addAll(authoredActivities);
        combined.addAll(approvedActivities);

        List<com.scutelnic.joinus.entity.Activity> calendarActivities = combined.stream()
            .filter(activity -> activity != null && activity.getId() != null && activity.getDate() != null)
            .collect(java.util.stream.Collectors.toMap(
                com.scutelnic.joinus.entity.Activity::getId,
                activity -> activity,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ))
            .values().stream()
            .sorted(Comparator
                .comparing(com.scutelnic.joinus.entity.Activity::getDate)
                .thenComparing(com.scutelnic.joinus.entity.Activity::getTime, Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

        model.addAttribute("calendarActivities", calendarActivities);
        model.addAttribute("isAuthenticatedUser", true);
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

    @GetMapping("/users/{id}")
    public String userProfile(@PathVariable Long id, Model model, Authentication authentication) {
        User user = userRepository.findById(id)
                .orElse(null);
        if (user == null) {
            return "redirect:/activities?missingUser";
        }

        UserReviewSummary ratingSummary = userReviewService.getSummary(user.getId());
        model.addAttribute("publicUser", user);
        model.addAttribute("publicUserAvatarUrl", resolveAvatarUrl(user));
        model.addAttribute("createdActivities", activityService.getByCreator(user.getId()));
        model.addAttribute("ratingSummary", ratingSummary);
        model.addAttribute("userReviews", userReviewService.getRecentForUser(user.getId(), 10));
        model.addAttribute("canLeaveReview", false);
        model.addAttribute("alreadyReviewed", false);
        model.addAttribute("isOwnPublicProfile", false);
        model.addAttribute("isAuthenticatedUser", isAuthenticated(authentication));

        if (!model.containsAttribute("reviewForm")) {
            UserReviewRequest reviewRequest = new UserReviewRequest();
            reviewRequest.setRating(5);
            model.addAttribute("reviewForm", reviewRequest);
        }

        if (isAuthenticated(authentication)) {
            userService.findByEmail(authentication.getName()).ifPresent(currentUser -> {
                boolean ownProfile = currentUser.getId().equals(user.getId());
                boolean alreadyReviewed = !ownProfile && userReviewService.hasReviewed(currentUser.getId(), user.getId());

                model.addAttribute("isOwnPublicProfile", ownProfile);
                model.addAttribute("alreadyReviewed", alreadyReviewed);
                model.addAttribute("canLeaveReview", !ownProfile && !alreadyReviewed);
            });
        }

        return "profile";
    }

    @PostMapping("/users/{id}/reviews")
    public String addUserReview(@PathVariable Long id,
                                @Valid @ModelAttribute("reviewForm") UserReviewRequest reviewForm,
                                BindingResult bindingResult,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (!isAuthenticated(authentication)) {
            redirectAttributes.addFlashAttribute("reviewError", "Autentifica-te pentru a lasa rating si feedback.");
            return "redirect:/users/" + id + "#section_user_reviews";
        }

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.reviewForm", bindingResult);
            redirectAttributes.addFlashAttribute("reviewForm", reviewForm);
            redirectAttributes.addFlashAttribute("reviewError", "Verifica rating-ul si feedback-ul introdus.");
            return "redirect:/users/" + id + "#section_user_reviews";
        }

        try {
            userReviewService.submitReview(authentication.getName(), id, reviewForm);
            redirectAttributes.addFlashAttribute("reviewSuccess", "Rating-ul si feedback-ul au fost salvate.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("reviewForm", reviewForm);
            redirectAttributes.addFlashAttribute("reviewError", ex.getMessage());
        }

        return "redirect:/users/" + id + "#section_user_reviews";
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
    public String activityDetail(@PathVariable Long id, Model model, Authentication authentication, HttpSession session) {
        return activityService.getById(id)
                .map(activity -> {
                    model.addAttribute("activity", activity);
                    model.addAttribute("otherActivities", resolveSidebarActivities(id, authentication));
                    model.addAttribute("canAccessChat", false);
                    model.addAttribute("isActivityCreator", false);
                    model.addAttribute("canRequestParticipation", false);
                    model.addAttribute("participationStatus", null);
                    model.addAttribute("pendingParticipationRequests", java.util.List.of());
                    model.addAttribute("approvedParticipationRequests", java.util.List.of());
                    String participationMessage = null;
                    String participationMessageType = null;
                    if (authentication != null && authentication.isAuthenticated()) {
                        String email = authentication.getName();
                        userRepository.findByEmail(email)
                                .ifPresent(user -> model.addAttribute("currentUserId", user.getId()));
                        boolean canAccessChat = participationService.canAccessChat(id, email);
                        boolean isCreator = participationService.isCreator(id, email);
                        boolean canRequestParticipation = participationService.canRequestParticipation(id, email);
                        ParticipationStatus participationStatus = participationService.getParticipationStatus(id, email);
                        model.addAttribute("canAccessChat", canAccessChat);
                        model.addAttribute("isActivityCreator", isCreator);
                        model.addAttribute("canRequestParticipation", canRequestParticipation);
                        model.addAttribute("participationStatus", participationStatus);
                        model.addAttribute("pendingParticipationRequests", participationService.getPendingRequestsForOrganizer(id, email));
                        model.addAttribute("approvedParticipationRequests", participationService.getApprovedParticipantsForViewer(id, email));
                        if (!isCreator) {
                            if (participationStatus == ParticipationStatus.PENDING) {
                                participationMessage = "Cerere trimisa. Status: in asteptare.";
                                participationMessageType = "warning";
                            } else if (participationStatus == ParticipationStatus.REJECTED) {
                                participationMessage = "Cererea a fost respinsa.";
                                participationMessageType = "danger";
                                session.removeAttribute(approvedNoticeSessionKey(id));
                            } else if (participationStatus == ParticipationStatus.EXCLUDED) {
                                participationMessage = "Ai fost exclus din aceasta activitate.";
                                participationMessageType = "danger";
                                session.removeAttribute(approvedNoticeSessionKey(id));
                            } else if (participationStatus == ParticipationStatus.BLOCKED) {
                                participationMessage = "Nu poti participa la aceasta activitate.";
                                participationMessageType = "danger";
                                session.removeAttribute(approvedNoticeSessionKey(id));
                            } else if (participationStatus == ParticipationStatus.APPROVED) {
                                Object approvedNoticeSeen = session.getAttribute(approvedNoticeSessionKey(id));
                                if (approvedNoticeSeen == null) {
                                    participationMessage = "Cererea a fost acceptata.";
                                    participationMessageType = "success";
                                    session.setAttribute(approvedNoticeSessionKey(id), Boolean.TRUE);
                                }
                            } else if (participationStatus == null) {
                                participationMessage = "Nu ai inca status de participare. Trimite o cerere.";
                                participationMessageType = "secondary";
                                session.removeAttribute(approvedNoticeSessionKey(id));
                            }
                        }
                    } else {
                        participationMessage = "Autentifica-te pentru a trimite o cerere de participare.";
                        participationMessageType = "secondary";
                    }

                    if (!model.containsAttribute("participationMessage") && participationMessage != null) {
                        model.addAttribute("participationMessage", participationMessage);
                    }
                    if (!model.containsAttribute("participationMessageType") && participationMessageType != null) {
                        model.addAttribute("participationMessageType", participationMessageType);
                    }
                    return "activity-detail";
                })
                .orElse("redirect:/activities?missing");
    }

    private List<com.scutelnic.joinus.entity.Activity> resolveSidebarActivities(Long currentActivityId,
                                                                                Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return List.of();
        }

        User user = userService.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return List.of();
        }

        List<com.scutelnic.joinus.entity.Activity> authoredActivities = activityService.getByCreator(user.getId());
        List<com.scutelnic.joinus.entity.Activity> approvedActivities =
                participationService.getApprovedActivitiesForUser(authentication.getName());

        List<com.scutelnic.joinus.entity.Activity> combined = new ArrayList<>(authoredActivities.size() + approvedActivities.size());
        combined.addAll(authoredActivities);
        combined.addAll(approvedActivities);

        Map<Long, com.scutelnic.joinus.entity.Activity> uniqueById = new LinkedHashMap<>();
        for (com.scutelnic.joinus.entity.Activity activity : combined) {
            if (activity == null || activity.getId() == null || activity.getId().equals(currentActivityId)) {
                continue;
            }
            uniqueById.putIfAbsent(activity.getId(), activity);
        }

        return uniqueById.values().stream()
                .sorted(Comparator.comparing(com.scutelnic.joinus.entity.Activity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .toList();
    }

    private String approvedNoticeSessionKey(Long activityId) {
        return "approved_notice_seen_activity_" + activityId;
    }

    private String resolveAvatarUrl(User user) {
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            return null;
        }
        return user.getAvatarUrl();
    }

    private void populateOwnProfileModel(Model model, User user, boolean mustCompleteBirthDate) {
        model.addAttribute("profileUser", user);
        model.addAttribute("profileAvatarUrl", resolveAvatarUrl(user));
        model.addAttribute("ratingSummary", userReviewService.getSummary(user.getId()));
        model.addAttribute("userReviews", userReviewService.getRecentForUser(user.getId(), 10));
        model.addAttribute("canLeaveReview", false);
        model.addAttribute("alreadyReviewed", false);
        model.addAttribute("isAuthenticatedUser", true);
        model.addAttribute("birthDateRequired", mustCompleteBirthDate);
        model.addAttribute("currentYear", LocalDate.now().getYear());
        model.addAttribute("createdActivities", activityService.getByCreator(user.getId()));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", toProfileUpdateRequest(user));
        } else {
            Object profileForm = model.getAttribute("profileForm");
            if (profileForm instanceof ProfileUpdateRequest request
                    && request.getBirthDate() == null
                    && user.getBirthDate() != null) {
                request.setBirthDate(user.getBirthDate());
            }
            if (profileForm instanceof ProfileUpdateRequest request
                    && (request.getAvatarUrl() == null || request.getAvatarUrl().isBlank())
                    && user.getAvatarUrl() != null) {
                request.setAvatarUrl(user.getAvatarUrl());
            }
        }
    }

    private ProfileUpdateRequest toProfileUpdateRequest(User user) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(user.getFullName());
        request.setBirthDate(user.getBirthDate());
        request.setBio(user.getBio());
        request.setAvatarUrl(user.getAvatarUrl());
        return request;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

}
