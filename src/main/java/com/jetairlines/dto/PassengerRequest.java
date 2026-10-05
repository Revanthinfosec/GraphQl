package com.jetairlines.dto;

import com.jetairlines.model.PassengerType;
import jakarta.validation.constraints.*;

public record PassengerRequest(
        @NotBlank @Size(max = 50) String firstName,
        @NotBlank @Size(max = 50) String lastName,
        @NotNull PassengerType passengerType,
        @Pattern(regexp = "^JM[0-9]{7}$", message = "must look like JM1234567") String loyaltyNumber
) {}
