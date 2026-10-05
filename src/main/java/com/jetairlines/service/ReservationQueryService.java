package com.jetairlines.service;

import com.jetairlines.dto.*;
import com.jetairlines.exception.ResourceNotFoundException;
import com.jetairlines.model.Reservation;
import com.jetairlines.model.SeatAssignment;
import com.jetairlines.repository.ReservationRepository;
import com.jetairlines.repository.SeatAssignmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * All "fetch reservation" reads (endpoints 1-11).
 * Returns DTOs built inside the transaction, so any entry point (REST today, GraphQL later)
 * can call these methods directly.
 */
@Service
@Transactional(readOnly = true)
public class ReservationQueryService {

    private final ReservationRepository reservations;
    private final SeatAssignmentRepository seats;

    public ReservationQueryService(ReservationRepository reservations, SeatAssignmentRepository seats) {
        this.reservations = reservations;
        this.seats = seats;
    }

    /** 1. Fetch by record locator (PNR) */
    public ReservationResponse getByRecordLocator(String recordLocator) {
        return ReservationResponse.from(find(recordLocator));
    }

    /** 2. Fetch by PNR + passenger last name (classic "manage my booking" lookup) */
    public ReservationResponse getByRecordLocatorAndLastName(String recordLocator, String lastName) {
        String pnr = normalize(recordLocator);
        String name = lastName.trim();
        return reservations.findByRecordLocator(pnr)
                .filter(r -> r.getPassengers().stream().anyMatch(p -> p.getLastName().equalsIgnoreCase(name)))
                .map(ReservationResponse::from)
                // same message whether the PNR or the name is wrong - don't leak which PNRs exist
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reservation " + pnr + " found for last name " + name));
    }

    /** 3. Fetch by JetMiles loyalty number (all reservations, newest first) */
    public List<ReservationResponse> getByLoyaltyNumber(String loyaltyNumber) {
        return reservations.findByLoyaltyNumber(loyaltyNumber.trim()).stream()
                .map(ReservationResponse::from).toList();
    }

    /** 4. Fetch by seat number on a flight + departure date */
    public ReservationResponse getBySeat(String flightNumber, LocalDate departureDate, String seatNumber) {
        LocalDateTime from = departureDate.atStartOfDay();
        List<SeatAssignment> found = seats.findActiveBySeat(flightNumber.trim(), from, from.plusDays(1), seatNumber.trim());
        if (found.isEmpty()) {
            throw new ResourceNotFoundException("No active reservation in seat " + seatNumber.trim().toUpperCase()
                    + " on " + flightNumber.trim().toUpperCase() + " departing " + departureDate);
        }
        return ReservationResponse.from(found.get(0).getReservation());
    }

    /** 5. Fetch by 13-digit e-ticket number */
    public ReservationResponse getByTicketNumber(String ticketNumber) {
        return reservations.findByTicketNumber(ticketNumber.trim())
                .map(ReservationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No reservation found for ticket number " + ticketNumber.trim()));
    }

    /** 6. Fetch by contact email (newest first) */
    public List<ReservationResponse> getByEmail(String email) {
        return reservations.findByContactEmailIgnoreCaseOrderByCreatedAtDesc(email.trim()).stream()
                .map(ReservationResponse::from).toList();
    }

    /** 7. Flight manifest: active reservations on a flight for a departure date */
    public List<ReservationResponse> getFlightManifest(String flightNumber, LocalDate departureDate) {
        LocalDateTime from = departureDate.atStartOfDay();
        return reservations.findActiveOnFlight(flightNumber.trim(), from, from.plusDays(1)).stream()
                .map(ReservationResponse::from).toList();
    }

    /** 8. Passengers of a reservation */
    public List<PassengerResponse> getPassengers(String recordLocator) {
        return getByRecordLocator(recordLocator).passengers();
    }

    /** 9. Flight segments of a reservation */
    public List<SegmentResponse> getSegments(String recordLocator) {
        return getByRecordLocator(recordLocator).segments();
    }

    /** 10. Seat assignments of a reservation */
    public List<SeatResponse> getSeats(String recordLocator) {
        return getByRecordLocator(recordLocator).seats();
    }

    /** 11. Itinerary summary (route, trip time, counts) */
    public ItinerarySummaryResponse getSummary(String recordLocator) {
        return ItinerarySummaryResponse.from(getByRecordLocator(recordLocator));
    }

    private Reservation find(String recordLocator) {
        String pnr = normalize(recordLocator);
        return reservations.findByRecordLocator(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + pnr));
    }

    static String normalize(String recordLocator) {
        return recordLocator.trim().toUpperCase();
    }
}
