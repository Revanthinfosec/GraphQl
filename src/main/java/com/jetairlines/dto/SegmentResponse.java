package com.jetairlines.dto;

import com.jetairlines.model.CabinClass;
import com.jetairlines.model.FlightSegment;
import com.jetairlines.model.SegmentStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * One flight leg. For codeshare legs (JT flight number, flown by a partner airline) the
 * operational disclosure fields tell the traveller who actually operates the flight.
 * Legs flown by JetAirlines itself should NOT carry these fields.
 */
public record SegmentResponse(Long id, String flightNumber, String origin, String destination,
                              LocalDateTime departureTime, LocalDateTime arrivalTime, long durationMinutes,
                              CabinClass cabin, SegmentStatus status,
                              String operationalDisclosureCarrier, String operationalDisclosureText) {

    public static SegmentResponse from(FlightSegment s) {
        String disclosureCarrier = null;
        String disclosureText = null;

        boolean codeshare = s.getFlightNumber().startsWith(s.getOperatingCarrier());
        if (codeshare) {
            disclosureCarrier = s.getOperatingCarrierName();
            disclosureText = "Operated by " + s.getOperatingCarrierName();
        }

        return new SegmentResponse(s.getId(), s.getFlightNumber(), s.getOrigin(), s.getDestination(),
                s.getDepartureTime(), s.getArrivalTime(),
                Duration.between(s.getDepartureTime(), s.getArrivalTime()).toMinutes(),
                s.getCabin(), s.getStatus(),
                disclosureCarrier, disclosureText);
    }
}
