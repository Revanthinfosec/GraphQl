package com.jetairlines.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** A reservation (PNR): who is travelling, on which flights, in which seats. */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 6)
    private String recordLocator;          // PNR, e.g. JTK7QP

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(nullable = false)
    private String contactEmail;

    private String contactPhone;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<Passenger> passengers = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("departureTime")
    private List<FlightSegment> segments = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<SeatAssignment> seats = new ArrayList<>();

    protected Reservation() {}

    public Reservation(String recordLocator, String contactEmail, String contactPhone) {
        this.recordLocator = recordLocator;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.createdAt = LocalDateTime.now();
    }

    public Passenger addPassenger(Passenger p) {
        p.setReservation(this);
        passengers.add(p);
        return p;
    }

    public FlightSegment addSegment(FlightSegment s) {
        s.setReservation(this);
        segments.add(s);
        return s;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        segments.forEach(s -> s.setStatus(SegmentStatus.CANCELLED));
    }

    public void updateContact(String email, String phone) {
        this.contactEmail = email;
        this.contactPhone = phone;
    }

    public Long getId() { return id; }
    public String getRecordLocator() { return recordLocator; }
    public ReservationStatus getStatus() { return status; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public List<Passenger> getPassengers() { return passengers; }
    public List<FlightSegment> getSegments() { return segments; }
    public List<SeatAssignment> getSeats() { return seats; }
}
