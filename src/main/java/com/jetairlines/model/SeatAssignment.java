package com.jetairlines.model;

import jakarta.persistence.*;

/** Seat for one passenger on one segment. */
@Entity
@Table(name = "seat_assignments",
       uniqueConstraints = @UniqueConstraint(columnNames = {"passenger_id", "segment_id"}))
public class SeatAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id")
    private Passenger passenger;

    @ManyToOne(optional = false)
    @JoinColumn(name = "segment_id")
    private FlightSegment segment;

    @Column(nullable = false, length = 3)
    private String seatNumber;             // e.g. 14A

    protected SeatAssignment() {}

    public SeatAssignment(Reservation reservation, Passenger passenger, FlightSegment segment, String seatNumber) {
        this.reservation = reservation;
        this.passenger = passenger;
        this.segment = segment;
        this.seatNumber = seatNumber;
    }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public Passenger getPassenger() { return passenger; }
    public FlightSegment getSegment() { return segment; }
    public String getSeatNumber() { return seatNumber; }
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
}
