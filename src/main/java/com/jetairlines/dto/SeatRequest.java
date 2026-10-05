package com.jetairlines.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record SeatRequest(
        @NotNull Long passengerId,
        @NotNull Long segmentId,
        @NotBlank @Pattern(regexp = "^[1-9][0-9]?[A-Ka-k]$", message = "must look like 14A") String seatNumber
) {}
