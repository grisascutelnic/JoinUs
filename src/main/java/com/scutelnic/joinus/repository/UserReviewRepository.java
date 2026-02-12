package com.scutelnic.joinus.repository;

import com.scutelnic.joinus.entity.UserReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserReviewRepository extends JpaRepository<UserReview, Long> {

    boolean existsByReviewerIdAndReviewedUserId(Long reviewerId, Long reviewedUserId);

    @EntityGraph(attributePaths = {"reviewer"})
    List<UserReview> findByReviewedUserIdOrderByCreatedAtDesc(Long reviewedUserId, Pageable pageable);

    long countByReviewedUserId(Long reviewedUserId);

    @Query("select avg(ur.rating) from UserReview ur where ur.reviewedUser.id = :reviewedUserId")
    Double findAverageRatingByReviewedUserId(@Param("reviewedUserId") Long reviewedUserId);
}
