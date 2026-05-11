package com.scutelnic.joinus.service;

import com.scutelnic.joinus.entity.Activity;
import com.scutelnic.joinus.entity.ActivityMemoryBook;
import com.scutelnic.joinus.entity.ActivityMemoryEntry;
import com.scutelnic.joinus.entity.ActivityMemoryPhoto;
import com.scutelnic.joinus.entity.ParticipationStatus;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.ActivityMemoryBookRepository;
import com.scutelnic.joinus.repository.ActivityMemoryEntryRepository;
import com.scutelnic.joinus.repository.ActivityParticipationRepository;
import com.scutelnic.joinus.repository.ActivityRepository;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ActivityMemoryService {

    private static final int MAX_FILES_PER_ENTRY = 12;

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final ActivityParticipationRepository participationRepository;
    private final ActivityMemoryBookRepository memoryBookRepository;
    private final ActivityMemoryEntryRepository memoryEntryRepository;
    private final CloudinaryService cloudinaryService;

    public ActivityMemoryService(ActivityRepository activityRepository,
                                 UserRepository userRepository,
                                 ActivityParticipationRepository participationRepository,
                                 ActivityMemoryBookRepository memoryBookRepository,
                                 ActivityMemoryEntryRepository memoryEntryRepository,
                                 CloudinaryService cloudinaryService) {
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.participationRepository = participationRepository;
        this.memoryBookRepository = memoryBookRepository;
        this.memoryEntryRepository = memoryEntryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional(readOnly = true)
    public List<ActivityMemoryEntry> getActivityEntries(Long activityId, boolean ascending) {
        if (ascending) {
            return memoryEntryRepository.findByActivityIdOrderByCreatedAtAsc(activityId);
        }
        return memoryEntryRepository.findByActivityIdOrderByCreatedAtDesc(activityId);
    }

    @Transactional(readOnly = true)
    public boolean canManageMemories(Long activityId, String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return false;
        }
        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);
        return isCreator(activity, user) || participationRepository.existsByActivityIdAndUserIdAndStatus(
                activityId,
                user.getId(),
                ParticipationStatus.APPROVED
        );
    }

    @Transactional(readOnly = true)
    public List<ActivityMemoryBook> getPublicBooks() {
        return memoryBookRepository.findByPublicUpdatedAtIsNotNullOrderByPublicUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<ActivityMemoryBook> findBookForActivity(Long activityId) {
        requireActivity(activityId);
        return memoryBookRepository.findByActivityId(activityId);
    }

    @Transactional
    public ActivityMemoryEntry addEntry(Long activityId,
                                        String userEmail,
                                        String content,
                                        List<MultipartFile> files,
                                        List<String> captions) {
        Activity activity = requireActivity(activityId);
        User user = requireUserByEmail(userEmail);

        if (!canManageMemories(activityId, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu poti adauga amintiri la aceasta activitate");
        }

        String normalizedContent = normalizeContent(content);
        List<MultipartFile> validFiles = normalizeFiles(files);

        if (normalizedContent.isEmpty() && validFiles.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adauga text sau cel putin o fotografie");
        }

        if (!validFiles.isEmpty() && !cloudinaryService.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cloudinary nu este configurat pentru upload");
        }

        ActivityMemoryEntry entry = new ActivityMemoryEntry();
        entry.setActivity(activity);
        entry.setAuthor(user);
        entry.setContent(normalizedContent.isEmpty() ? "Amintire fara comentariu" : normalizedContent);

        List<ActivityMemoryPhoto> photos = new ArrayList<>();
        for (int i = 0; i < validFiles.size(); i++) {
            MultipartFile file = validFiles.get(i);
            String imageUrl;
            try {
                imageUrl = cloudinaryService.uploadImage(file);
            } catch (Exception ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nu am putut incarca una dintre imagini");
            }

            if (imageUrl == null || imageUrl.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagine invalida");
            }

            ActivityMemoryPhoto photo = new ActivityMemoryPhoto();
            photo.setEntry(entry);
            photo.setImageUrl(imageUrl);
            photo.setDisplayOrder(i);
            photo.setCaption(resolveCaption(captions, i));
            photos.add(photo);
        }

        entry.setPhotos(photos);
        ActivityMemoryEntry saved = memoryEntryRepository.save(entry);

        ActivityMemoryBook book = ensureBook(activity);
        book.setUpdatedAt(java.time.LocalDateTime.now());
        memoryBookRepository.save(book);

        return saved;
    }

    @Transactional
    public ActivityMemoryBook publishBook(Long activityId, String userEmail) {
        if (!canManageMemories(activityId, userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu poti publica aceasta carte");
        }
        Activity activity = requireActivity(activityId);
        ActivityMemoryBook book = ensureBook(activity);
        book.setPublicUpdatedAt(java.time.LocalDateTime.now());
        return memoryBookRepository.save(book);
    }

    private ActivityMemoryBook ensureBook(Activity activity) {
        return memoryBookRepository.findByActivityId(activity.getId())
                .orElseGet(() -> {
                    ActivityMemoryBook created = new ActivityMemoryBook();
                    created.setActivity(activity);
                    return memoryBookRepository.save(created);
                });
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() > 1200) {
            return trimmed.substring(0, 1200);
        }
        return trimmed;
    }

    private List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<MultipartFile> valid = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (valid.size() > MAX_FILES_PER_ENTRY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Poti adauga maximum " + MAX_FILES_PER_ENTRY + " imagini odata");
        }
        return valid;
    }

    private String resolveCaption(List<String> captions, int index) {
        if (captions == null || index >= captions.size()) {
            return null;
        }
        String value = captions.get(index);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 240) {
            return trimmed.substring(0, 240);
        }
        return trimmed;
    }

    private boolean isCreator(Activity activity, User user) {
        return activity.getCreator() != null && activity.getCreator().getId().equals(user.getId());
    }

    private Activity requireActivity(Long activityId) {
        return activityRepository.findWithCreatorById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activitatea nu a fost gasita"));
    }

    private User requireUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
