package com.jetairlines.service;

import com.jetairlines.dto.*;
import com.jetairlines.exception.BusinessRuleException;
import com.jetairlines.exception.ResourceNotFoundException;
import com.jetairlines.model.*;
import com.jetairlines.repository.PassengerRepository;
import com.jetairlines.repository.ReservationRepository;
import com.jetairlines.repository.SeatAssignmentRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * All reservation writes (endpoints 12-15).
 * @Validated: request validation also runs in the service layer, not only in the controllers.
 */
@Service
@Transactional
@Validated
public class ReservationCommandService {

    private static final String PNR_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I confusion
    private static final String TICKET_PREFIX = "999";                         // fictional airline code
    private final SecureRandom random = new SecureRandom();

    private final ReservationRepository reservations;
    private final PassengerRepository passengers;
    private final SeatAssignmentRepository seats;

    public ReservationCommandService(ReservationRepository reservations, PassengerRepository passengers,
                                     SeatAssignmentRepository seats) {
        this.reservations = reservations;
        this.passengers = passengers;
        this.seats = seats;
    }

    /** 12. Create reservation: issues a PNR and one e-ticket per passenger */
    public ReservationResponse create(@Valid CreateReservationRequest req) {
        long adults = req.passengers().stream().filter(p -> p.passengerType() == PassengerType.ADULT).count();
        long infants = req.passengers().stream().filter(p -> p.passengerType() == PassengerType.INFANT).count();
        if (adults == 0) {
            throw new IllegalArgumentException("At least one ADULT passenger is required");
        }
        if (infants > adults) {
            throw new IllegalArgumentException("Each infant must travel with an adult");
        }
        for (SegmentRequest s : req.segments()) {
            if (s.origin().equals(s.destination())) {
                throw new IllegalArgumentException("Segment " + s.flightNumber() + ": origin and destination must differ");
            }
            if (!s.arrivalTime().isAfter(s.departureTime())) {
                throw new IllegalArgumentException("Segment " + s.flightNumber() + ": arrival must be after departure");
            }
        }

        Reservation r = new Reservation(newRecordLocator(), req.contactEmail().trim().toLowerCase(), req.contactPhone());
        Set<String> issued = new HashSet<>();
        for (PassengerRequest p : req.passengers()) {
            r.addPassenger(new Passenger(p.firstName().trim(), p.lastName().trim(), p.passengerType(),
                    p.loyaltyNumber(), newTicketNumber(issued)));
        }
        for (SegmentRequest s : req.segments()) {
            r.addSegment(new FlightSegment(s.flightNumber(), s.origin(), s.destination(),
                    s.departureTime(), s.arrivalTime(), s.cabin()));
        }
        return ReservationResponse.from(reservations.save(r));
    }

    /** 13. Update contact email / phone */
    public ReservationResponse updateContact(String recordLocator, @Valid UpdateContactRequest req) {
        Reservation r = findActive(recordLocator);
        r.updateContact(req.contactEmail().trim().toLowerCase(), req.contactPhone());
        reservations.flush();
        return ReservationResponse.from(r);
    }

    /** 14. Assign or change a passenger's seat on one segment */
    public ReservationResponse assignSeat(String recordLocator, @Valid SeatRequest req) {
        Reservation r = findActive(recordLocator);
        Passenger passenger = r.getPassengers().stream()
                .filter(p -> p.getId().equals(req.passengerId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Passenger " + req.passengerId() + " is not on reservation " + r.getRecordLocator()));
        FlightSegment segment = r.getSegments().stream()
                .filter(s -> s.getId().equals(req.segmentId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Segment " + req.segmentId() + " is not on reservation " + r.getRecordLocator()));

        if (passenger.getPassengerType() == PassengerType.INFANT) {
            throw new BusinessRuleException("Infants travel on an adult's lap and cannot be assigned a seat");
        }
        if (!segment.getDepartureTime().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Flight " + segment.getFlightNumber() + " has already departed");
        }

        String seat = req.seatNumber().toUpperCase();
        LocalDateTime day = segment.getDepartureTime().toLocalDate().atStartOfDay();
        boolean takenBySomeoneElse = seats.findActiveBySeat(segment.getFlightNumber(), day, day.plusDays(1), seat)
                .stream().anyMatch(a -> !a.getPassenger().getId().equals(passenger.getId()));
        if (takenBySomeoneElse) {
            throw new BusinessRuleException("Seat " + seat + " on " + segment.getFlightNumber() + " is already taken");
        }

        r.getSeats().stream()
                .filter(a -> a.getPassenger().getId().equals(passenger.getId())
                          && a.getSegment().getId().equals(segment.getId()))
                .findFirst()
                .ifPresentOrElse(
                        existing -> existing.setSeatNumber(seat),                                  // change seat
                        () -> r.getSeats().add(new SeatAssignment(r, passenger, segment, seat))); // new seat
        reservations.flush();
        return ReservationResponse.from(r);
    }

    /** 15. Cancel reservation (all segments) */
    public ReservationResponse cancel(String recordLocator) {
        Reservation r = find(recordLocator);
        if (r.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Reservation " + r.getRecordLocator() + " is already cancelled");
        }
        if (r.getSegments().stream().anyMatch(s -> !s.getDepartureTime().isAfter(LocalDateTime.now()))) {
            throw new BusinessRuleException("Reservation " + r.getRecordLocator()
                    + " has a flight that already departed and cannot be cancelled");
        }
        r.cancel();
        reservations.flush();
        return ReservationResponse.from(r);
    }

    private Reservation find(String recordLocator) {
        String pnr = ReservationQueryService.normalize(recordLocator);
        return reservations.findByRecordLocator(pnr)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + pnr));
    }

    private Reservation findActive(String recordLocator) {
        Reservation r = find(recordLocator);
        if (r.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessRuleException("Reservation " + r.getRecordLocator() + " is cancelled and cannot be changed");
        }
        return r;
    }

    private String newRecordLocator() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) sb.append(PNR_CHARS.charAt(random.nextInt(PNR_CHARS.length())));
            ref = sb.toString();
        } while (reservations.existsByRecordLocator(ref));
        return ref;
    }

    private String newTicketNumber(Set<String> issuedInThisRequest) {
        String ticket;
        do {
            StringBuilder sb = new StringBuilder(TICKET_PREFIX);
            for (int i = 0; i < 10; i++) sb.append(random.nextInt(10));
            ticket = sb.toString();
        } while (issuedInThisRequest.contains(ticket) || passengers.existsByTicketNumber(ticket));
        issuedInThisRequest.add(ticket);
        return ticket;
    }
}
