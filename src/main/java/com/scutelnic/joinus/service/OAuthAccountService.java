package com.scutelnic.joinus.service;

import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OAuthAccountService {

    private static final PasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public OAuthAccountService(UserRepository userRepository, CloudinaryService cloudinaryService) {
        this.userRepository = userRepository;
        this.cloudinaryService = cloudinaryService;
    }

    @Transactional
    public GoogleUpsertResult upsertGoogleUser(String email, String fullName, String pictureUrl) {
        String normalizedEmail = email.toLowerCase().trim();
        String resolvedName = (fullName == null || fullName.isBlank())
                ? normalizedEmail.split("@")[0]
                : fullName.trim();
        String resolvedPictureUrl = normalizeNullable(pictureUrl);
        final boolean[] created = {false};

        userRepository.findByEmail(normalizedEmail)
                .ifPresentOrElse(existingUser -> {
                    if (existingUser.getFullName() == null || existingUser.getFullName().isBlank()) {
                        existingUser.setFullName(resolvedName);
                    }

                    String currentAvatarUrl = normalizeNullable(existingUser.getAvatarUrl());
                    if (currentAvatarUrl == null && resolvedPictureUrl != null) {
                        existingUser.setAvatarUrl(toStableAvatarUrl(resolvedPictureUrl));
                    } else if (isGoogleAvatarUrl(currentAvatarUrl)) {
                        // One-time migration for old accounts that still store Google URLs.
                        existingUser.setAvatarUrl(toStableAvatarUrl(resolvedPictureUrl != null ? resolvedPictureUrl : currentAvatarUrl));
                    }
                }, () -> {
                    User user = new User();
                    user.setFullName(resolvedName);
                    user.setEmail(normalizedEmail);
                    user.setAvatarUrl(toStableAvatarUrl(resolvedPictureUrl));
                    user.setPassword(PASSWORD_ENCODER.encode(UUID.randomUUID().toString()));
                    userRepository.save(user);
                    created[0] = true;
                });

        return new GoogleUpsertResult(created[0]);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toStableAvatarUrl(String pictureUrl) {
        if (pictureUrl == null) {
            return null;
        }
        if (isCloudinaryUrl(pictureUrl) || !cloudinaryService.isConfigured()) {
            return pictureUrl;
        }
        try {
            String mirrored = cloudinaryService.uploadImageFromUrl(pictureUrl);
            return normalizeNullable(mirrored) != null ? mirrored : pictureUrl;
        } catch (Exception ignored) {
            return pictureUrl;
        }
    }

    private boolean isGoogleAvatarUrl(String url) {
        return url != null && url.contains("googleusercontent.com");
    }

    private boolean isCloudinaryUrl(String url) {
        return url != null && url.contains("res.cloudinary.com");
    }

    public record GoogleUpsertResult(boolean created) {}
}
