package com.jetairlines.dto;

import com.jetairlines.model.Passenger;
import com.jetairlines.model.PassengerType;

public record PassengerResponse(Long id, String firstName, String lastName, PassengerType passengerType,
                                String loyaltyNumber, String ticketNumber) {
    public static PassengerResponse from(Passenger p) {
        return new PassengerResponse(p.getId(), p.getFirstName(), p.getLastName(), p.getPassengerType(),
                p.getLoyaltyNumber(), p.getTicketNumber());
    }
}
