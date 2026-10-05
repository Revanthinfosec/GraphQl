package com.jetairlines.model;

import jakarta.persistence.*;

@Entity
@Table(name = "passengers")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassengerType passengerType;

    @Column(length = 9)
    private String loyaltyNumber;          // JetMiles number, e.g. JM1234567 (optional)

    @Column(nullable = false, unique = true, length = 13)
    private String ticketNumber;           // 13-digit e-ticket, e.g. 9990000000001

    protected Passenger() {}

    public Passenger(String firstName, String lastName, PassengerType passengerType,
                     String loyaltyNumber, String ticketNumber) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.passengerType = passengerType;
        this.loyaltyNumber = loyaltyNumber;
        this.ticketNumber = ticketNumber;
    }

    void setReservation(Reservation reservation) { this.reservation = reservation; }

    public Long getId() { return id; }
    public Reservation getReservation() { return reservation; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return firstName + " " + lastName; }
    public PassengerType getPassengerType() { return passengerType; }
    public String getLoyaltyNumber() { return loyaltyNumber; }
    public String getTicketNumber() { return ticketNumber; }
}
