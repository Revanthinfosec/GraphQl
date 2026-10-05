package com.jetairlines.repository;

import com.jetairlines.model.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {
    boolean existsByTicketNumber(String ticketNumber);
}
