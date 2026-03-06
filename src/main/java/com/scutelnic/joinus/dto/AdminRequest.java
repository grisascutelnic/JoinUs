package com.scutelnic.joinus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRequest {

    @NotBlank
    private String targetType;

    private Long recipientUserId;

    @NotBlank
    @Size(max = 160)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String message;

    @Size(max = 255)
    private String link;
}
