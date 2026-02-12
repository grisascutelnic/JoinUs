package com.scutelnic.joinus.service;

import com.scutelnic.joinus.dto.UserReviewRequest;
import com.scutelnic.joinus.dto.UserReviewSummary;
import com.scutelnic.joinus.entity.User;
import com.scutelnic.joinus.entity.UserReview;
import com.scutelnic.joinus.repository.UserRepository;
import com.scutelnic.joinus.repository.UserReviewRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserReviewService {

    private final UserReviewRepository userReviewRepository;
    private final UserRepository userRepository;

    public UserReviewService(UserReviewRepository userReviewRepository, UserRepository userRepository) {
        this.userReviewRepository = userReviewRepository;
        this.userRepository = userRepository;
    }

    public UserReviewSummary getSummary(Long reviewedUserId) {
        long count = userReviewRepository.countByReviewedUserId(reviewedUserId);
        if (count == 0) {
            return new UserReviewSummary(0.0, 0);
        }

        Double average = userReviewRepository.findAverageRatingByReviewedUserId(reviewedUserId);
        return new UserReviewSummary(average != null ? average : 0.0, count);
    }

    public List<UserReview> getRecentForUser(Long reviewedUserId, int limit) {
        return userReviewRepository.findByReviewedUserIdOrderByCreatedAtDesc(
                reviewedUserId,
                PageRequest.of(0, limit)
        );
    }

    public boolean hasReviewed(Long reviewerId, Long reviewedUserId) {
        return userReviewRepository.existsByReviewerIdAndReviewedUserId(reviewerId, reviewedUserId);
    }

    @Transactional
    public void submitReview(String reviewerEmail, Long reviewedUserId, UserReviewRequest request) {
        User reviewer = userRepository.findByEmail(normalizeEmail(reviewerEmail))
                .orElseThrow(() -> new IllegalArgumentException("Trebuie sa fii autentificat pentru a lasa feedback."));
        User reviewedUser = userRepository.findById(reviewedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizatorul evaluat nu exista."));

        if (reviewer.getId().equals(reviewedUser.getId())) {
            throw new IllegalArgumentException("Nu poti lasa rating propriului profil.");
        }

        if (userReviewRepository.existsByReviewerIdAndReviewedUserId(reviewer.getId(), reviewedUserId)) {
            throw new IllegalArgumentException("Ai lasat deja rating si feedback acestui utilizator.");
        }

        UserReview review = new UserReview();
        review.setReviewer(reviewer);
        review.setReviewedUser(reviewedUser);
        review.setRating(request.getRating());
        review.setFeedback(request.getFeedback().trim());

        try {
            userReviewRepository.save(review);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Ai lasat deja rating si feedback acestui utilizator.");
        }
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.toLowerCase().trim();
    }
}
