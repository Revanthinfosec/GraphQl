package com.jetairlines;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exercises all 15 REST endpoints against the seeded data (see DataSeeder).
 * Run with:  mvn test
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ReservationApiIntegrationTest {

    private static final String API = "/api/reservations";
    private static final String D1 = LocalDate.now().plusDays(1).toString();
    private static final String NEXT_WEEK = LocalDate.now().plusDays(7).toString();

    @Autowired MockMvc mvc;

    private String json(String url) throws Exception {
        return mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    // ---------------- Fetch (1-11) ----------------

    @Test // 1
    void fetchByRecordLocator() throws Exception {
        mvc.perform(get(API + "/jtk7qp"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.recordLocator").value("JTK7QP"))
           .andExpect(jsonPath("$.status").value("ACTIVE"))
           .andExpect(jsonPath("$.passengers", hasSize(2)))
           .andExpect(jsonPath("$.segments", hasSize(2)))
           .andExpect(jsonPath("$.seats", hasSize(4)));
        mvc.perform(get(API + "/ZZZZZZ"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.message").value("Reservation not found: ZZZZZZ"));
    }

    @Test // 2
    void fetchByRecordLocatorAndLastName() throws Exception {
        mvc.perform(get(API + "/search").param("recordLocator", "JTK7QP").param("lastName", "martinez"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.passengers[0].firstName").value("Ava"));
        mvc.perform(get(API + "/search").param("recordLocator", "JTK7QP").param("lastName", "Smith"))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.message").value("No reservation JTK7QP found for last name Smith"));
        mvc.perform(get(API + "/search").param("recordLocator", "JTK7QP"))
           .andExpect(status().isBadRequest());
    }

    @Test // 3
    void fetchByLoyaltyNumber() throws Exception {
        mvc.perform(get(API + "/by-loyalty/jm7654321"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[*].recordLocator", containsInAnyOrder("JTM4XR", "JTP2NV")));
        mvc.perform(get(API + "/by-loyalty/JM0000000"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test // 4
    void fetchBySeat() throws Exception {
        mvc.perform(get(API + "/by-seat").param("flightNumber", "jt101").param("departureDate", D1).param("seatNumber", "2a"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.recordLocator").value("JTM4XR"));
        // 20C belonged to a cancelled reservation -> not found
        mvc.perform(get(API + "/by-seat").param("flightNumber", "JT101").param("departureDate", D1).param("seatNumber", "20C"))
           .andExpect(status().isNotFound());
        mvc.perform(get(API + "/by-seat").param("flightNumber", "JT101").param("departureDate", "tomorrow").param("seatNumber", "2A"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message").value("Invalid value for parameter 'departureDate'"));
    }

    @Test // 5
    void fetchByTicketNumber() throws Exception {
        mvc.perform(get(API + "/by-ticket/9990000000004"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.recordLocator").value("JTZ8LC"));
        mvc.perform(get(API + "/by-ticket/9999999999999"))
           .andExpect(status().isNotFound());
    }

    @Test // 6
    void fetchByEmail() throws Exception {
        mvc.perform(get(API + "/by-email").param("email", "PRIYA.NAIR@example.com"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test // 7
    void flightManifest() throws Exception {
        // JTK7QP + JTM4XR are on JT101; cancelled JTC9WD is excluded
        mvc.perform(get(API + "/by-flight").param("flightNumber", "JT101").param("departureDate", D1))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[0].recordLocator").value("JTK7QP"))
           .andExpect(jsonPath("$[1].recordLocator").value("JTM4XR"));
    }

    @Test // 8
    void passengers() throws Exception {
        mvc.perform(get(API + "/JTZ8LC/passengers"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[1].passengerType").value("INFANT"))
           .andExpect(jsonPath("$[0].ticketNumber").value("9990000000004"));
    }

    @Test // 9
    void segments() throws Exception {
        mvc.perform(get(API + "/JTK7QP/segments"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].flightNumber").value("JT101"))
           .andExpect(jsonPath("$[0].durationMinutes").value(275))
           .andExpect(jsonPath("$[1].origin").value("JFK"));
    }

    @Test // 10
    void seats() throws Exception {
        mvc.perform(get(API + "/JTK7QP/seats"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(4)))
           .andExpect(jsonPath("$[0].seatNumber").value("14A"))
           .andExpect(jsonPath("$[0].passengerName").value("Ava Martinez"));
    }

    @Test // 11
    void summary() throws Exception {
        mvc.perform(get(API + "/JTK7QP/summary"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.route", contains("DFW", "JFK", "LHR")))
           .andExpect(jsonPath("$.passengerCount").value(2))
           .andExpect(jsonPath("$.segmentCount").value(2))
           .andExpect(jsonPath("$.finalDestination").value("LHR"));
    }

    // ---------------- Changes (12-15) ----------------

    private String newReservation(String passengersJson) {
        return """
               {"contactEmail":"Noah.Kim@Example.com","contactPhone":"+1 972 555 0199",
                "passengers":%s,
                "segments":[{"flightNumber":"JT700","origin":"DFW","destination":"SFO",
                             "departureTime":"%sT10:00:00","arrivalTime":"%sT11:55:00","cabin":"ECONOMY"}]}
               """.formatted(passengersJson, NEXT_WEEK, NEXT_WEEK);
    }

    @Test // 12
    void createReservation() throws Exception {
        String body = newReservation("""
                [{"firstName":"Noah","lastName":"Kim","passengerType":"ADULT","loyaltyNumber":"JM5555555"}]
                """);
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body))
           .andExpect(status().isCreated())
           .andExpect(header().string("Location", containsString(API + "/")))
           .andExpect(jsonPath("$.recordLocator", matchesPattern("^[A-Z2-9]{6}$")))
           .andExpect(jsonPath("$.contactEmail").value("noah.kim@example.com"))
           .andExpect(jsonPath("$.passengers[0].ticketNumber", matchesPattern("^999[0-9]{10}$")));
        // JetMiles lookup now finds the cancelled JTC9WD and the new one
        mvc.perform(get(API + "/by-loyalty/JM5555555")).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test // 12 - validation and business rules
    void createReservationRejectsBadInput() throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{\"contactEmail\":\"not-an-email\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.contactEmail").exists())
           .andExpect(jsonPath("$.fieldErrors.passengers").exists())
           .andExpect(jsonPath("$.fieldErrors.segments").exists());
        // no adult on the booking
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(newReservation("""
                [{"firstName":"Leo","lastName":"Kim","passengerType":"CHILD"}]
                """)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.message").value("At least one ADULT passenger is required"));
    }

    @Test // 13
    void updateContact() throws Exception {
        mvc.perform(put(API + "/JTK7QP/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactEmail\":\"Ava.New@Example.com\",\"contactPhone\":\"+1 214 555 0199\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.contactEmail").value("ava.new@example.com"));
        mvc.perform(put(API + "/JTC9WD/contact").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contactEmail\":\"noah@example.com\"}"))
           .andExpect(status().isConflict());
    }

    @Test // 14
    void assignAndChangeSeats() throws Exception {
        String jtz = json(API + "/JTZ8LC");
        Number liam = JsonPath.read(jtz, "$.passengers[0].id");
        Number mia = JsonPath.read(jtz, "$.passengers[1].id");
        Number sfoOrd = JsonPath.read(jtz, "$.segments[0].id");
        String seat = "{\"passengerId\":%s,\"segmentId\":%s,\"seatNumber\":\"%s\"}";

        // change Liam's seat 1C -> 2d (stored upper-case)
        mvc.perform(put(API + "/JTZ8LC/seats").contentType(MediaType.APPLICATION_JSON).content(seat.formatted(liam, sfoOrd, "2d")))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.seats", hasSize(1)))
           .andExpect(jsonPath("$.seats[0].seatNumber").value("2D"));
        // lap infant can't have a seat
        mvc.perform(put(API + "/JTZ8LC/seats").contentType(MediaType.APPLICATION_JSON).content(seat.formatted(mia, sfoOrd, "2E")))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.message", containsString("Infants")));
        // bad seat format
        mvc.perform(put(API + "/JTZ8LC/seats").contentType(MediaType.APPLICATION_JSON).content(seat.formatted(liam, sfoOrd, "ZZ")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.seatNumber").exists());

        String jtk = json(API + "/JTK7QP");
        Number ava = JsonPath.read(jtk, "$.passengers[0].id");
        Number jt101 = JsonPath.read(jtk, "$.segments[0].id");
        // 2A on JT101 belongs to Priya (another reservation)
        mvc.perform(put(API + "/JTK7QP/seats").contentType(MediaType.APPLICATION_JSON).content(seat.formatted(ava, jt101, "2A")))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.message").value("Seat 2A on JT101 is already taken"));
        // 20C was released when JTC9WD was cancelled
        mvc.perform(put(API + "/JTK7QP/seats").contentType(MediaType.APPLICATION_JSON).content(seat.formatted(ava, jt101, "20C")))
           .andExpect(status().isOk());
        // segment from another reservation
        mvc.perform(put(API + "/JTK7QP/seats").contentType(MediaType.APPLICATION_JSON).content(seat.formatted(ava, sfoOrd, "5A")))
           .andExpect(status().isNotFound());
    }

    @Test // 15
    void cancelReservation() throws Exception {
        mvc.perform(post(API + "/JTP2NV/cancel"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("CANCELLED"))
           .andExpect(jsonPath("$.cancelledAt").exists())
           .andExpect(jsonPath("$.segments[0].status").value("CANCELLED"));
        mvc.perform(post(API + "/JTP2NV/cancel"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.message").value("Reservation JTP2NV is already cancelled"));
        mvc.perform(post(API + "/ZZZZZZ/cancel")).andExpect(status().isNotFound());
    }
}
