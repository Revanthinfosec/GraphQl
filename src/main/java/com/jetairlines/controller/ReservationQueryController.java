package com.jetairlines.controller;

import com.jetairlines.dto.*;
import com.jetairlines.service.ReservationQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Fetch Reservation - read endpoints 1-11. */
@RestController
@RequestMapping("/api/reservations")
public class ReservationQueryController {

    private final ReservationQueryService queries;

    public ReservationQueryController(ReservationQueryService queries) { this.queries = queries; }

    /** 1. Fetch by record locator (PNR) */
    @GetMapping("/{recordLocator}")
    public ReservationResponse byRecordLocator(@PathVariable String recordLocator) {
        return queries.getByRecordLocator(recordLocator);
    }

    /** 2. Fetch by PNR + last name */
    @GetMapping("/search")
    public ReservationResponse byRecordLocatorAndLastName(@RequestParam String recordLocator,
                                                          @RequestParam String lastName) {
        return queries.getByRecordLocatorAndLastName(recordLocator, lastName);
    }

    /** 3. Fetch by JetMiles loyalty number */
    @GetMapping("/by-loyalty/{loyaltyNumber}")
    public List<ReservationResponse> byLoyaltyNumber(@PathVariable String loyaltyNumber) {
        return queries.getByLoyaltyNumber(loyaltyNumber);
    }

    /** 4. Fetch by seat number on a flight + departure date */
    @GetMapping("/by-seat")
    public ReservationResponse bySeat(@RequestParam String flightNumber,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate,
                                      @RequestParam String seatNumber) {
        return queries.getBySeat(flightNumber, departureDate, seatNumber);
    }

    /** 5. Fetch by e-ticket number */
    @GetMapping("/by-ticket/{ticketNumber}")
    public ReservationResponse byTicketNumber(@PathVariable String ticketNumber) {
        return queries.getByTicketNumber(ticketNumber);
    }

    /** 6. Fetch by contact email */
    @GetMapping("/by-email")
    public List<ReservationResponse> byEmail(@RequestParam String email) {
        return queries.getByEmail(email);
    }

    /** 7. Flight manifest (active reservations on a flight/date) */
    @GetMapping("/by-flight")
    public List<ReservationResponse> byFlight(@RequestParam String flightNumber,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate departureDate) {
        return queries.getFlightManifest(flightNumber, departureDate);
    }

    /** 8. Passengers on a reservation */
    @GetMapping("/{recordLocator}/passengers")
    public List<PassengerResponse> passengers(@PathVariable String recordLocator) {
        return queries.getPassengers(recordLocator);
    }

    /** 9. Flight segments on a reservation */
    @GetMapping("/{recordLocator}/segments")
    public List<SegmentResponse> segments(@PathVariable String recordLocator) {
        return queries.getSegments(recordLocator);
    }

    /** 10. Seat assignments on a reservation */
    @GetMapping("/{recordLocator}/seats")
    public List<SeatResponse> seats(@PathVariable String recordLocator) {
        return queries.getSeats(recordLocator);
    }

    /** 11. Itinerary summary */
    @GetMapping("/{recordLocator}/summary")
    public ItinerarySummaryResponse summary(@PathVariable String recordLocator) {
        return queries.getSummary(recordLocator);
    }
}
