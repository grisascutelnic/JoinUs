package com.scutelnic.joinus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @Past
    private LocalDate birthDate;

    @Size(max = 300)
    private String bio;

    @Size(max = 500)
    private String avatarUrl;
}
