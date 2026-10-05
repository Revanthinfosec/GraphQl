package com.jetairlines.dto;

import com.jetairlines.model.CabinClass;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record SegmentRequest(
        @NotBlank @Pattern(regexp = "^JT[0-9]{1,4}$", message = "must look like JT101") String flightNumber,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter airport code") String origin,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter airport code") String destination,
        @NotNull @Future LocalDateTime departureTime,
        @NotNull LocalDateTime arrivalTime,
        @NotNull CabinClass cabin
) {}
