package com.scutelnic.joinus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank
    @Size(max = 120)
    private String fullName;

    @Past
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @Size(max = 300)
    private String bio;

    @Size(max = 500)
    private String avatarUrl;
}
