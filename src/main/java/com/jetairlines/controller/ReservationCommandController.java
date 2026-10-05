package com.jetairlines.controller;

import com.jetairlines.dto.*;
import com.jetairlines.service.ReservationCommandService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Reservation changes - write endpoints 12-15. */
@RestController
@RequestMapping("/api/reservations")
public class ReservationCommandController {

    private final ReservationCommandService commands;

    public ReservationCommandController(ReservationCommandService commands) { this.commands = commands; }

    /** 12. Create reservation */
    @PostMapping
    public ResponseEntity<ReservationResponse> create(@Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse body = commands.create(request);
        return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{recordLocator}").buildAndExpand(body.recordLocator()).toUri()).body(body);
    }

    /** 13. Update contact details */
    @PutMapping("/{recordLocator}/contact")
    public ReservationResponse updateContact(@PathVariable String recordLocator,
                                             @Valid @RequestBody UpdateContactRequest request) {
        return commands.updateContact(recordLocator, request);
    }

    /** 14. Assign / change a seat */
    @PutMapping("/{recordLocator}/seats")
    public ReservationResponse assignSeat(@PathVariable String recordLocator,
                                          @Valid @RequestBody SeatRequest request) {
        return commands.assignSeat(recordLocator, request);
    }

    /** 15. Cancel reservation */
    @PostMapping("/{recordLocator}/cancel")
    public ReservationResponse cancel(@PathVariable String recordLocator) {
        return commands.cancel(recordLocator);
    }
}
