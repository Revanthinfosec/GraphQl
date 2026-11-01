# JetAirlines FetchReservation API (practice project)

Spring Boot 4 REST API for a **fictional** airline's reservation lookup ("fetch reservation") service.
15 endpoints, layered `controller → service → repository`.

## Run
Requires JDK 17+ and Maven 3.9+.
```bash
mvn spring-boot:run     # http://localhost:8081  (8081 so it can run next to airline-api on 8080)
mvn test                # integration tests for all 15 endpoints
```
- Swagger UI: http://localhost:8081/swagger-ui.html
- H2 console: http://localhost:8081/h2-console  (JDBC URL `jdbc:h2:mem:jetairlines`, user `sa`)

## Endpoints

| # | Method | Path | Purpose |
|---|---|---|---|
| 1 | GET | `/api/reservations/{recordLocator}` | Fetch by PNR |
| 2 | GET | `/api/reservations/search?recordLocator=JTK7QP&lastName=Martinez` | Fetch by PNR + last name |
| 3 | GET | `/api/reservations/by-loyalty/{loyaltyNumber}` | Fetch by JetMiles number |
| 4 | GET | `/api/reservations/by-seat?flightNumber=JT101&departureDate=YYYY-MM-DD&seatNumber=2A` | Fetch by seat number |
| 5 | GET | `/api/reservations/by-ticket/{ticketNumber}` | Fetch by e-ticket number |
| 6 | GET | `/api/reservations/by-email?email=...` | Fetch by contact email |
| 7 | GET | `/api/reservations/by-flight?flightNumber=JT101&departureDate=YYYY-MM-DD` | Flight manifest |
| 8 | GET | `/api/reservations/{recordLocator}/passengers` | Passengers |
| 9 | GET | `/api/reservations/{recordLocator}/segments` | Flight segments |
| 10 | GET | `/api/reservations/{recordLocator}/seats` | Seat assignments |
| 11 | GET | `/api/reservations/{recordLocator}/summary` | Itinerary summary (route, trip time) |
| 12 | POST | `/api/reservations` | Create reservation (issues PNR + e-tickets) |
| 13 | PUT | `/api/reservations/{recordLocator}/contact` | Update contact details |
| 14 | PUT | `/api/reservations/{recordLocator}/seats` | Assign / change a seat |
| 15 | POST | `/api/reservations/{recordLocator}/cancel` | Cancel reservation |

## Sample data (dates are relative to today)

| PNR | Passengers (JetMiles / ticket) | Flights (seats) |
|---|---|---|
| JTK7QP | Ava Martinez `JM1234567` / 9990000000001, Leo Martinez (child) / …002 | JT101 DFW→JFK +1 day (14A, 14B), JT202 JFK→LHR +1 day (32A, 32B) |
| JTM4XR | Priya Nair `JM7654321` / …003 | JT101 DFW→JFK +1 day, Business (2A) |
| JTZ8LC | Liam Chen / …004, Mia Chen (lap infant) / …005 | JT305 SFO→ORD +3 days, First (Liam 1C) |
| JTP2NV | Priya Nair `JM7654321` / …006 | JT410 LHR→DFW +5 days (no seat) |
| JTC9WD | Noah Kim `JM5555555` / …007 — **CANCELLED** | JT101 DFW→JFK +1 day (20C, now free) |

## Business rules
- PNRs are 6 characters without look-alikes (0/O/1/I). Lookups ignore case.
- Search by PNR + last name returns the same 404 whether the PNR or the name is wrong.
- Lookups by loyalty number, email and flight return a list, which may be empty. Lookups by PNR, seat and ticket return 404 when nothing matches.
- Seat lookup and manifest ignore cancelled reservations.
- Create: at least one ADULT, no more infants than adults, arrival after departure, origin ≠ destination.
- Seats: format `14A`; one seat per passenger per segment; a seat can't be taken twice on the same flight/day; lap infants can't get a seat.
- Cancelled reservations can't be changed. A reservation with a departed flight can't be cancelled.

Errors return one JSON shape: `400` validation (with `fieldErrors`), `404` not found, `409` business rule.
```json
{"timestamp":"...","status":409,"error":"Conflict","message":"Seat 2A on JT101 is already taken","path":"/api/reservations/JTK7QP/seats"}
```

## Examples
```bash
curl localhost:8081/api/reservations/JTK7QP
curl "localhost:8081/api/reservations/by-seat?flightNumber=JT101&departureDate=$(date -v+1d +%F)&seatNumber=2A"   # macOS date
curl -X PUT localhost:8081/api/reservations/JTZ8LC/seats -H 'Content-Type: application/json' \
  -d '{"passengerId":4,"segmentId":4,"seatNumber":"2D"}'
```

## Project layout
```
src/main/java/com/jetairlines
├── model/        JPA entities: Reservation, Passenger, FlightSegment, SeatAssignment + enums
├── repository/   Spring Data JPA (custom JPQL for loyalty / seat / flight lookups)
├── service/      ReservationQueryService (reads 1-11), ReservationCommandService (writes 12-15)
├── controller/   ReservationQueryController, ReservationCommandController
├── dto/          Request/response records (returned by the services)
├── exception/    Error types + REST error handler
└── config/       Sample-data seeder
```
