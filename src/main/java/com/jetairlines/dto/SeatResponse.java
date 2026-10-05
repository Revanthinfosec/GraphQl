package com.jetairlines.dto;

import com.jetairlines.model.SeatAssignment;

public record SeatResponse(Long id, Long passengerId, String passengerName,
                           Long segmentId, String flightNumber, String seatNumber) {
    public static SeatResponse from(SeatAssignment a) {
        return new SeatResponse(a.getId(), a.getPassenger().getId(), a.getPassenger().getFullName(),
                a.getSegment().getId(), a.getSegment().getFlightNumber(), a.getSeatNumber());
    }
}
