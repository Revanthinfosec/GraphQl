# JET-101: Itinerary summary shows wrong total trip time

| Field | Value |
|---|---|
| Type | Bug |
| Priority | High |
| Story points | 2 |
| Assignee | Revanth |
| Status | To Do |
| Component | Fetch Reservation API - endpoint 11 (summary) |
| Reported by | Customer Support |

## User story
As a **traveller**, I want the itinerary summary to show my **total trip time correctly**,
so that I know how long my whole journey will take, door to door.

## Description
Customer Ava Martinez (PNR **JTK7QP**, DFW -> JFK -> LHR) reports that the total trip
time in her itinerary summary looks too short.

## Steps to reproduce
1. Start the app in Debug mode.
2. Call `GET http://localhost:8081/api/reservations/JTK7QP/summary`
3. Compare `totalTripMinutes` with the time between `firstDeparture` and `lastArrival`.

## Expected result
`totalTripMinutes` = minutes from the first flight's departure to the **last flight's arrival**.

## Actual result
`totalTripMinutes` does not match `firstDeparture` -> `lastArrival`.

## Acceptance criteria
- [ ] For JTK7QP, `totalTripMinutes` equals the minutes between `firstDeparture` and `lastArrival`.
- [ ] Single-leg trips (e.g. JTM4XR) also show the correct total.
- [ ] Root cause documented below.

## Debugging hints
- Start at `ReservationQueryService.getSummary(...)`, step into (F7) the summary builder.
- Inspect `first` and `last` in the Variables panel.

## Dev notes (fill in)
- **Root cause:**
- **File / line:**
- **Fix:**
- **How I verified:**
