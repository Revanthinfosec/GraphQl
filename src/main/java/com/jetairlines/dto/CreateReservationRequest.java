package com.jetairlines.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateReservationRequest(
        @NotBlank @Email String contactEmail,
        @Pattern(regexp = "^\\+?[0-9 ()-]{7,20}$", message = "must be a phone number") String contactPhone,
        @NotEmpty @Size(max = 9) @Valid List<PassengerRequest> passengers,
        @NotEmpty @Size(max = 4) @Valid List<SegmentRequest> segments
) {}
