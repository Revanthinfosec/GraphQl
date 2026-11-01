# JET-102: Manage My Booking lookup fails for lowercase last name

| Field | Value |
|---|---|
| Type | Bug |
| Priority | Critical |
| Story points | 1 |
| Assignee | Revanth |
| Status | To Do |
| Component | Fetch Reservation API - endpoint 2 (search by PNR + last name) |
| Reported by | Customer Support (multiple complaints) |

## User story
As a **traveller**, I want to find my booking with my PNR and last name **however I type my name**,
so that I can manage my trip without errors.

## Description
Customers using "Manage My Booking" get *No reservation found* even though their PNR and
last name are correct. Seems to happen when they type their name in lowercase.

## Steps to reproduce
1. Start the app in Debug mode.
2. Call `GET http://localhost:8081/api/reservations/search?recordLocator=JTK7QP&lastName=martinez`
3. Then call it again with `lastName=Martinez`.

## Expected result
Both calls return reservation JTK7QP (200 OK).

## Actual result
`lastName=martinez` returns 404 Not Found.

## Acceptance criteria
- [ ] `martinez`, `Martinez`, and `MARTINEZ` all return JTK7QP.
- [ ] A wrong last name (e.g. `smith`) still returns 404 with the same generic message.
- [ ] Root cause documented below.

## Debugging hints
- Endpoint 2 in `ReservationQueryService`.
- The logic is inside a lambda: choose the inner lambda (`p -> ...`) when setting the breakpoint.
- Use Evaluate Expression (Option + F8) on the comparison.

## Dev notes (fill in)
- **Root cause:**
- **File / line:**
- **Fix:**
- **How I verified:**
