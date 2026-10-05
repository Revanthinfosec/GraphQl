package com.jetairlines.repository;

import com.jetairlines.model.SeatAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SeatAssignmentRepository extends JpaRepository<SeatAssignment, Long> {

    /** Seat holders on an active reservation for this flight + departure day. */
    @Query("""
           SELECT a FROM SeatAssignment a
           WHERE UPPER(a.segment.flightNumber) = UPPER(:flightNumber)
             AND a.segment.departureTime >= :from AND a.segment.departureTime < :to
             AND UPPER(a.seatNumber) = UPPER(:seatNumber)
             AND a.reservation.status = com.jetairlines.model.ReservationStatus.ACTIVE
           """)
    List<SeatAssignment> findActiveBySeat(@Param("flightNumber") String flightNumber,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to,
                                          @Param("seatNumber") String seatNumber);
}
