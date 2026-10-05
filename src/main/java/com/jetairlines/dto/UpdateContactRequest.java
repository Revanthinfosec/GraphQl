package com.jetairlines.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateContactRequest(
        @NotBlank @Email String contactEmail,
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "must be a phone number") String contactPhone
) {}
