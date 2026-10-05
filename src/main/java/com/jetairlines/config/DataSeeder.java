package com.jetairlines.config;

import com.jetairlines.model.*;
import com.jetairlines.repository.ReservationRepository;
import com.jetairlines.repository.SeatAssignmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Sample reservations, scheduled relative to today so they never go stale.
 *
 *  PNR     Passengers (JetMiles / ticket)                        Flights (seat)
 *  JTK7QP  Ava Martinez JM1234567 / 9990000000001,                JT101 DFW-JFK +1d (14A,14B)
 *          Leo Martinez (child)  / 9990000000002                  JT202 JFK-LHR +1d (32A,32B)
 *  JTM4XR  Priya Nair  JM7654321 / 9990000000003                  JT101 DFW-JFK +1d BUSINESS (2A)
 *  JTZ8LC  Liam Chen             / 9990000000004,                 JT305 SFO-ORD +3d FIRST (Liam 1C)
 *          Mia Chen (infant)     / 9990000000005
 *  JTP2NV  Priya Nair  JM7654321 / 9990000000006                  JT410 LHR-DFW +5d (no seat)
 *  JTC9WD  Noah Kim    JM5555555 / 9990000000007  ** CANCELLED ** JT101 DFW-JFK +1d (20C)
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(ReservationRepository reservations, SeatAssignmentRepository seats) {
        return args -> {
            if (reservations.count() > 0) return;

            LocalDate d1 = LocalDate.now().plusDays(1);
            LocalDate d3 = LocalDate.now().plusDays(3);
            LocalDate d5 = LocalDate.now().plusDays(5);

            // JTK7QP - family of two, connecting DFW -> JFK -> LHR
            Reservation r1 = new Reservation("JTK7QP", "ava.martinez@example.com", "+1 214 555 0101");
            Passenger ava = r1.addPassenger(new Passenger("Ava", "Martinez", PassengerType.ADULT, "JM1234567", "9990000000001"));
            Passenger leo = r1.addPassenger(new Passenger("Leo", "Martinez", PassengerType.CHILD, null, "9990000000002"));
            FlightSegment r1a = r1.addSegment(seg("JT101", "DFW", "JFK", d1, "08:00", 275, CabinClass.ECONOMY));
            FlightSegment r1b = r1.addSegment(seg("JT202", "JFK", "LHR", d1, "18:30", 430, CabinClass.ECONOMY));

            // JTM4XR - Priya, business class on the same JT101
            Reservation r2 = new Reservation("JTM4XR", "priya.nair@example.com", "+44 20 7946 0958");
            Passenger priya = r2.addPassenger(new Passenger("Priya", "Nair", PassengerType.ADULT, "JM7654321", "9990000000003"));
            FlightSegment r2a = r2.addSegment(seg("JT101", "DFW", "JFK", d1, "08:00", 275, CabinClass.BUSINESS));

            // JTZ8LC - Liam with a lap infant
            Reservation r3 = new Reservation("JTZ8LC", "liam.chen@example.com", "+1 415 555 0177");
            Passenger liam = r3.addPassenger(new Passenger("Liam", "Chen", PassengerType.ADULT, null, "9990000000004"));
            r3.addPassenger(new Passenger("Mia", "Chen", PassengerType.INFANT, null, "9990000000005"));
            FlightSegment r3a = r3.addSegment(seg("JT305", "SFO", "ORD", d3, "09:00", 245, CabinClass.FIRST));

            // JTP2NV - Priya's second trip (loyalty lookup returns 2)
            Reservation r4 = new Reservation("JTP2NV", "priya.nair@example.com", "+44 20 7946 0958");
            r4.addPassenger(new Passenger("Priya", "Nair", PassengerType.ADULT, "JM7654321", "9990000000006"));
            r4.addSegment(seg("JT410", "LHR", "DFW", d5, "11:00", 630, CabinClass.PREMIUM_ECONOMY));

            // JTC9WD - cancelled; its seat 20C on JT101 is free again
            Reservation r5 = new Reservation("JTC9WD", "noah.kim@example.com", "+1 972 555 0199");
            Passenger noah = r5.addPassenger(new Passenger("Noah", "Kim", PassengerType.ADULT, "JM5555555", "9990000000007"));
            FlightSegment r5a = r5.addSegment(seg("JT101", "DFW", "JFK", d1, "08:00", 275, CabinClass.ECONOMY));
            r5.cancel();

            reservations.saveAll(List.of(r1, r2, r3, r4, r5));

            // Seats are saved after the reservations so passengers/segments already have ids
            seats.saveAll(List.of(
                new SeatAssignment(r1, ava, r1a, "14A"),
                new SeatAssignment(r1, leo, r1a, "14B"),
                new SeatAssignment(r1, ava, r1b, "32A"),
                new SeatAssignment(r1, leo, r1b, "32B"),
                new SeatAssignment(r2, priya, r2a, "2A"),
                new SeatAssignment(r3, liam, r3a, "1C"),
                new SeatAssignment(r5, noah, r5a, "20C")
            ));
        };
    }

    private static FlightSegment seg(String flight, String from, String to, LocalDate day, String time,
                                     int minutes, CabinClass cabin) {
        LocalDateTime dep = LocalDateTime.of(day, LocalTime.parse(time));
        return new FlightSegment(flight, from, to, dep, dep.plusMinutes(minutes), cabin);
    }
}
