# JET-103: Operational disclosure shown on wrong flights

| Field | Value |
|---|---|
| Type | Bug |
| Priority | High (regulatory: DOT codeshare disclosure) |
| Story points | 3 |
| Assignee | Revanth |
| Status | To Do |
| Component | Fetch Reservation API - segment response |
| Reported by | Compliance team |

## User story
As a **traveller booked on a codeshare flight**, I want to see **which airline actually operates
my flight**, so that I go to the right terminal and check-in counter.

As **JetAirlines**, we do **not** want the disclosure on flights we operate ourselves.

## Description
Segments now carry `operationalDisclosureCarrier` and `operationalDisclosureText`.
Compliance found that the fields appear on the wrong flights.

## Steps to reproduce
1. Start the app in Debug mode.
2. Call `GET http://localhost:8081/api/reservations/JTR6PA/segments` (3 partner-operated flights).
3. Call `GET http://localhost:8081/api/reservations/JTK7QP/segments` (JetAirlines-operated flights).

## Expected result
| Flight | Operated by | Disclosure shown? |
|---|---|---|
| JT7001 | American Airlines | Yes - "Operated by American Airlines" |
| JT7002 | Japan Airlines | Yes - "Operated by Japan Airlines" |
| JT7003 | Singapore Airlines | Yes - "Operated by Singapore Airlines" |
| JT101 / JT202 / JT305 / JT410 | JetAirlines | No - fields absent |

## Actual result
- All 3 partner flights are **missing** the disclosure fields.
- JetAirlines flights **show** "Operated by JetAirlines".

## Acceptance criteria
- [ ] JT7001, JT7002, JT7003 show the correct carrier name and text.
- [ ] No JetAirlines-operated segment contains either disclosure field.
- [ ] The fix works for any future partner carrier (no hard-coded airline names).
- [ ] Root cause documented below.

## Debugging hints
- Breakpoint at the start of `SegmentResponse.from(...)`; it pauses once per flight leg.
- Conditional breakpoint: `s.getFlightNumber().equals("JT7002")`.
- Watch `s.getFlightNumber()`, `s.getOperatingCarrier()` and `codeshare`.

## Dev notes (fill in)
- **Root cause:**
- **File / line:**
- **Fix:**
- **How I verified:**
