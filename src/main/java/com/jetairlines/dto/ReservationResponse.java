package com.jetairlines.dto;

import com.jetairlines.model.Reservation;
import com.jetairlines.model.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Full reservation view. Built inside a service transaction (lazy collections are loaded there). */
public record ReservationResponse(
        Long id,
        String recordLocator,
        ReservationStatus status,
        String contactEmail,
        String contactPhone,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt,
        List<PassengerResponse> passengers,
        List<SegmentResponse> segments,
        List<SeatResponse> seats
) {
    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(r.getId(), r.getRecordLocator(), r.getStatus(),
                r.getContactEmail(), r.getContactPhone(), r.getCreatedAt(), r.getCancelledAt(),
                r.getPassengers().stream().map(PassengerResponse::from).toList(),
                r.getSegments().stream().map(SegmentResponse::from).toList(),
                r.getSeats().stream().map(SeatResponse::from).toList());
    }
}
