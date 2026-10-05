package com.jetairlines.dto;

import com.jetairlines.model.ReservationStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Computed trip overview, e.g. route DFW -> JFK -> LHR. */
public record ItinerarySummaryResponse(
        String recordLocator,
        ReservationStatus status,
        int passengerCount,
        int segmentCount,
        String origin,
        String finalDestination,
        List<String> route,
        LocalDateTime firstDeparture,
        LocalDateTime lastArrival,
        long totalTripMinutes
) {
    public static ItinerarySummaryResponse from(ReservationResponse r) {
        List<SegmentResponse> legs = r.segments();          // ordered by departure time
        SegmentResponse first = legs.get(0);
        SegmentResponse last = legs.get(legs.size() - 1);
        List<String> route = new ArrayList<>();
        route.add(first.origin());
        legs.forEach(s -> route.add(s.destination()));
        return new ItinerarySummaryResponse(r.recordLocator(), r.status(), r.passengers().size(), legs.size(),
                first.origin(), last.destination(), List.copyOf(route),
                first.departureTime(), last.arrivalTime(),
                Duration.between(first.departureTime(), last.arrivalTime()).toMinutes());
    }
}
