package com.jetairlines.repository;

import com.jetairlines.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByRecordLocator(String recordLocator);

    boolean existsByRecordLocator(String recordLocator);

    List<Reservation> findByContactEmailIgnoreCaseOrderByCreatedAtDesc(String contactEmail);

    @Query("""
           SELECT r FROM Reservation r
           WHERE r.id IN (SELECT p.reservation.id FROM Passenger p
                          WHERE UPPER(p.loyaltyNumber) = UPPER(:loyaltyNumber))
           ORDER BY r.createdAt DESC
           """)
    List<Reservation> findByLoyaltyNumber(@Param("loyaltyNumber") String loyaltyNumber);

    @Query("""
           SELECT r FROM Reservation r
           WHERE r.id IN (SELECT p.reservation.id FROM Passenger p WHERE p.ticketNumber = :ticketNumber)
           """)
    Optional<Reservation> findByTicketNumber(@Param("ticketNumber") String ticketNumber);

    /** Active reservations with a segment on this flight, departing in [from, to). */
    @Query("""
           SELECT r FROM Reservation r
           WHERE r.status = com.jetairlines.model.ReservationStatus.ACTIVE
             AND r.id IN (SELECT s.reservation.id FROM FlightSegment s
                          WHERE UPPER(s.flightNumber) = UPPER(:flightNumber)
                            AND s.departureTime >= :from AND s.departureTime < :to)
           ORDER BY r.recordLocator
           """)
    List<Reservation> findActiveOnFlight(@Param("flightNumber") String flightNumber,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
