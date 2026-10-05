package com.jetairlines.dto;

import com.jetairlines.model.CabinClass;
import com.jetairlines.model.FlightSegment;
import com.jetairlines.model.SegmentStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public record SegmentResponse(Long id, String flightNumber, String origin, String destination,
                              LocalDateTime departureTime, LocalDateTime arrivalTime, long durationMinutes,
                              CabinClass cabin, SegmentStatus status) {
    public static SegmentResponse from(FlightSegment s) {
        return new SegmentResponse(s.getId(), s.getFlightNumber(), s.getOrigin(), s.getDestination(),
                s.getDepartureTime(), s.getArrivalTime(),
                Duration.between(s.getDepartureTime(), s.getArrivalTime()).toMinutes(),
                s.getCabin(), s.getStatus());
    }
}
