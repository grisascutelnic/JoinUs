package com.scutelnic.joinus.service;

import com.scutelnic.joinus.dto.ProfileUpdateRequest;
import com.scutelnic.joinus.dto.RegisterRequest;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Parolele nu coincid.");
        }

        String normalizedEmail = request.getEmail().toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Exista deja un cont cu acest email.");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBirthDate(request.getBirthDate());
        user.setBio(request.getBio());

        userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email));
    }

    @Transactional
    public User updateProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul nu exista."));

        user.setFullName(request.getFullName().trim());
        user.setBirthDate(request.getBirthDate());
        user.setBio(normalizeNullable(request.getBio()));
        user.setAvatarUrl(normalizeNullable(request.getAvatarUrl()));

        return user;
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase().trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
