package com.scutelnic.joinus.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public class ActivityDto {

    @NotBlank
    @Size(max = 80)
    private String title;

    @NotBlank
    @Size(max = 240)
    private String description;

    @NotNull
    @FutureOrPresent
    private LocalDate date;

    @NotNull
    private LocalTime time;

    @NotBlank
    @Size(max = 120)
    private String location;

    @NotBlank
    @Size(max = 160)
    private String address;

    @NotNull
    @Min(1)
    @Max(200)
    private Integer capacity;

    @Size(max = 40)
    private String category;

    @Size(max = 120)
    private String tags;

    private String imageChoice;

    @Size(max = 500)
    private String imageUrl;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getImageChoice() {
        return imageChoice;
    }

    public void setImageChoice(String imageChoice) {
        this.imageChoice = imageChoice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
