package com.scutelnic.joinus.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserReviewRequest {

    @NotNull(message = "Alege un rating.")
    @Min(value = 1, message = "Rating-ul minim este 1.")
    @Max(value = 5, message = "Rating-ul maxim este 5.")
    private Integer rating;

    @NotBlank(message = "Feedback-ul este obligatoriu.")
    @Size(max = 1000, message = "Feedback-ul poate avea maximum 1000 de caractere.")
    private String feedback;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
