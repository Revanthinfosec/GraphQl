package com.jetairlines.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One flight leg of a reservation, e.g. JT101 DFW -> JFK. */
@Entity
@Table(name = "flight_segments")
public class FlightSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false, length = 6)
    private String flightNumber;

    @Column(nullable = false, length = 3)
    private String origin;

    @Column(nullable = false, length = 3)
    private String destination;

    @Column(nullable = false)
    private LocalDateTime departureTime;

    @Column(nullable = false)
    private LocalDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CabinClass cabin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SegmentStatus status = SegmentStatus.CONFIRMED;

    /** Airline that actually flies the plane, e.g. "JT" (JetAirlines) or a codeshare partner like "AA". */
    @Column(nullable = false, length = 2)
    private String operatingCarrier = "JT";

    @Column(nullable = false, length = 40)
    private String operatingCarrierName = "JetAirlines";

    protected FlightSegment() {}

    public FlightSegment(String flightNumber, String origin, String destination,
                         LocalDateTime departureTime, LocalDateTime arrivalTime, CabinClass cabin) {
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.cabin = cabin;
    }

    /** Codeshare leg: sold under a JT flight number but operated by a partner airline. */
    public FlightSegment(String flightNumber, String origin, String destination,
                         LocalDateTime departureTime, LocalDateTime arrivalTime, CabinClass cabin,
                         String operatingCarrier, String operatingCarrierName) {
        this(flightNumber, origin, destination, departureTime, arrivalTime, cabin);
        this.operatingCarrier = operatingCarrier;
        this.operatingCarrierName = operatingCarrierName;
    }

    void setReservation(Reservation reservation) { this.reservation = reservation; }
    void setStatus(SegmentStatus status) { this.status = status; }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public String getFlightNumber() { return flightNumber; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public CabinClass getCabin() { return cabin; }
    public SegmentStatus getStatus() { return status; }
    public String getOperatingCarrier() { return operatingCarrier; }
    public String getOperatingCarrierName() { return operatingCarrierName; }
}
