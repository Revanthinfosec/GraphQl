# Learning GraphQL on Spring Boot: JetAirlines Fetch Reservation API

A hands-on guide for adding a **GraphQL layer** to this Spring Boot app and exposing **2 of its existing endpoints** through it.

> **How to use this guide:** do one step at a time. Type the code yourself instead of copy-pasting it, and read the "Why" note under each block. Each step ends with a **checkpoint**. Don't move on until the checkpoint passes.

---

## Table of contents

0. [What the task means](#step-0--what-the-task-means)
1. [Add the dependency](#step-1--add-the-dependency)
2. [Write the schema (the contract)](#step-2--write-the-schema-the-contract)
3. [Write the GraphQL controller (the resolvers)](#step-3--write-the-graphql-controller-the-resolvers)
4. [Run it and explore with GraphiQL](#step-4--run-it-and-explore-with-graphiql)
5. [Error handling](#step-5--error-handling-the-big-mindset-change)
6. [Automated tests](#step-6--automated-tests)
7. [Self-check questions](#self-check-questions)
8. [Troubleshooting](#troubleshooting)
9. [What to learn next](#what-to-learn-next)

**Progress checklist**

- [ ] Step 0: I can explain how GraphQL differs from REST, in my own words
- [ ] Step 1: The GraphQL dependency is added and the app still starts
- [ ] Step 2: `schema.graphqls` is written and the app starts without schema errors
- [ ] Step 3: The controller is written, and the startup log reports no unmapped fields
- [ ] Step 4: Both queries return data in GraphiQL and in Postman
- [ ] Step 5: An unknown PNR returns a `NOT_FOUND` GraphQL error
- [ ] Step 6: `mvn test` passes, including the new GraphQL tests

---

## Step 0: What the task means

### REST (what this app has today)

15 URLs. Each one returns a **fixed JSON shape** chosen by the server.

```
GET /api/reservations/JTK7QP              -> full reservation (all passengers, segments, seats)
GET /api/reservations/by-loyalty/JM7654321 -> list of full reservations
```

If a mobile app only needs the reservation `status`, it still downloads everything.

### GraphQL

| | REST | GraphQL |
|---|---|---|
| URLs | One per resource or operation | **One**: `POST /graphql` |
| Who picks the response shape | The server | **The client**, field by field |
| Contract | OpenAPI (often generated from the code) | **Schema** (`.graphqls`), usually written first |
| Errors | HTTP status codes (404, 409…) | Usually HTTP 200 with an `errors` array in the body |
| Reads / writes | GET / POST, PUT, DELETE | `query` / `mutation` |

Example: the client asks for exactly what it wants…

```graphql
{
  reservationByRecordLocator(recordLocator: "JTK7QP") {
    status
    passengers { firstName }
  }
}
```

…and gets exactly that back:

```json
{
  "data": {
    "reservationByRecordLocator": {
      "status": "ACTIVE",
      "passengers": [ { "firstName": "Ava" }, { "firstName": "Leo" } ]
    }
  }
}
```

### So what does "expose 2 endpoints through GraphQL" mean?

> Choose 2 existing operations and make each one callable as a **field on the GraphQL `Query` type**.
> **Nothing gets rewritten**, and the REST API keeps working.

### The core architectural idea

GraphQL is **another door into the same building**:

```
REST client ───► ReservationQueryController     (@RestController) ──┐
                                                                    ├──► ReservationQueryService ──► Repositories ──► H2
GraphQL client ► ReservationGraphQlController   (@Controller)    ───┘
```

The **service layer** already holds all the business logic. The GraphQL controller is a thin adapter on top of it, just like the REST controller.

This codebase is a good fit: `ReservationQueryService` returns fully built DTO records (`ReservationResponse`, etc.) **inside a read-only transaction**, so GraphQL can read every nested field without lazy-loading errors.

### Which 2 endpoints?

If whoever assigned the task named specific endpoints, use those. Otherwise this guide uses:

| REST endpoint | Why it's a good learning pick | GraphQL field |
|---|---|---|
| #1 `GET /api/reservations/{recordLocator}` | Returns **one object**; can be "not found" | `reservationByRecordLocator` |
| #3 `GET /api/reservations/by-loyalty/{loyaltyNumber}` | Returns a **list**; may be empty | `reservationsByLoyaltyNumber` |

### Vocabulary

| Term | Meaning | Spring/REST equivalent you already know |
|---|---|---|
| **Schema / SDL** | The `.graphqls` file that defines types and operations | OpenAPI spec |
| **`type`** | The shape of an object | A DTO record |
| **`Query`** | The root type for reads | GET endpoints |
| **`Mutation`** | The root type for writes | POST / PUT / DELETE |
| **Field** | One property of a type, or one operation on `Query` | A record component, or one endpoint |
| **Argument** | An input to a field | `@PathVariable`, `@RequestParam` |
| **Resolver / DataFetcher** | The code that produces a field's value | A controller method |
| **`!`** | Not null | `required = true` |
| **`[T]`** | A list of T | `List<T>` |
| **Scalar** | A leaf value: `Int`, `Float`, `String`, `Boolean`, `ID` | Primitive / String |

**✅ Checkpoint 0:** Explain out loud why one GraphQL URL can replace several GET URLs, and why the service layer doesn't change.

---

## Step 1: Add the dependency

Open `pom.xml` and add this inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql</artifactId>
</dependency>
```

And in the test section:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-graphql-test</artifactId>
    <scope>test</scope>
</dependency>
```

**Why:**

- It brings in **graphql-java**, the engine that parses queries, validates them against the schema and executes them.
- It brings in **Spring for GraphQL**, the Spring integration, which:
  1. loads every `*.graphqls` file from `src/main/resources/graphql/`,
  2. registers the `/graphql` endpoint on the existing web server (port **8081**),
  3. finds the `@Controller` methods that resolve each schema field.
- No `<version>` tag is needed, because the Spring Boot parent (4.1.1) manages versions.

**✅ Checkpoint 1:** Run `mvn spring-boot:run`. The app still starts. Startup may complain that no schema was found; Step 2 fixes that.

---

## Step 2: Write the schema (the contract)

Create **`src/main/resources/graphql/schema.graphqls`**:

```graphql
type Query {
  "Endpoint #1: fetch a reservation by PNR (record locator)"
  reservationByRecordLocator(recordLocator: String!): Reservation

  "Endpoint #3: all reservations for a JetMiles loyalty number"
  reservationsByLoyaltyNumber(loyaltyNumber: String!): [Reservation!]!
}

type Reservation {
  id: ID!
  recordLocator: String!
  status: ReservationStatus!
  contactEmail: String
  contactPhone: String
  createdAt: String
  cancelledAt: String
  passengers: [Passenger!]!
  segments: [Segment!]!
  seats: [Seat!]!
}

type Passenger {
  id: ID!
  firstName: String!
  lastName: String!
  passengerType: PassengerType!
  loyaltyNumber: String
  ticketNumber: String
}

type Segment {
  id: ID!
  flightNumber: String!
  origin: String!
  destination: String!
  departureTime: String!
  arrivalTime: String!
  durationMinutes: Int!
  cabin: CabinClass!
  status: SegmentStatus!
}

type Seat {
  id: ID!
  passengerId: ID!
  passengerName: String!
  segmentId: ID!
  flightNumber: String!
  seatNumber: String!
}

enum ReservationStatus { ACTIVE CANCELLED }
enum PassengerType     { ADULT CHILD INFANT }
enum CabinClass        { ECONOMY PREMIUM_ECONOMY BUSINESS FIRST }
enum SegmentStatus     { CONFIRMED CANCELLED }
```

### How this was derived (do this yourself for any DTO)

Put each schema type next to its Java record:

| Schema type | Java source |
|---|---|
| `Reservation` | `dto/ReservationResponse.java` |
| `Passenger` | `dto/PassengerResponse.java` |
| `Segment` | `dto/SegmentResponse.java` |
| `Seat` | `dto/SeatResponse.java` |
| enums | `model/ReservationStatus`, `PassengerType`, `CabinClass`, `SegmentStatus` |

### Why it's written this way

1. **Field names match the record component names exactly.**
   You will write **no Java code** for `Reservation.status`, `Passenger.firstName` or any other nested field. graphql-java's default *property data fetcher* calls `reservation.status()` on your record automatically. This is the main reason reusing existing DTOs costs so little.

2. **GraphQL has only 5 built-in scalars:** `Int` (32-bit), `Float`, `String`, `Boolean` and `ID`. It has **no** `Long` or `LocalDateTime`.
   - The `Long` ids map to **`ID`**. That's the convention, and they serialize as strings.
   - `LocalDateTime` maps to **`String`**. It serializes through `toString()` as ISO, e.g. `2026-10-17T08:00`. That's good enough for learning; production apps usually add a custom `DateTime` scalar (see [What to learn next](#what-to-learn-next)).
   - `long durationMinutes` maps to `Int`. That's safe because the values are small.

3. **Enum values must match the Java enum constant names exactly**, or the app fails at runtime.

4. **Nullability is part of your contract:**
   - `reservationByRecordLocator` returns `Reservation` with **no `!`**, so "not found" can come back as `null` plus an error (Step 5).
   - `reservationsByLoyaltyNumber` returns `[Reservation!]!`: the list itself is never null but may be empty, and it never contains null items. That matches what the service already does.

5. **Model only what's reachable from your queries.** There's no `ItinerarySummary` type, because neither of the 2 queries returns one. Don't model all 15 endpoints up front.

**✅ Checkpoint 2:** The app starts. In the startup log, look for a **"GraphQL schema inspection"** report. At this point it should say that `Query.reservationByRecordLocator` and `Query.reservationsByLoyaltyNumber` are **unmapped**. That's expected, because no resolvers exist yet. Seeing that report is the lesson.

---

## Step 3: Write the GraphQL controller (the resolvers)

Create **`src/main/java/com/jetairlines/graphql/ReservationGraphQlController.java`**:

```java
package com.jetairlines.graphql;

import com.jetairlines.dto.ReservationResponse;
import com.jetairlines.service.ReservationQueryService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** GraphQL entry point for reservation reads. Delegates to the same service as the REST API. */
@Controller
public class ReservationGraphQlController {

    private final ReservationQueryService queries;

    public ReservationGraphQlController(ReservationQueryService queries) { this.queries = queries; }

    /** Query.reservationByRecordLocator  (REST #1) */
    @QueryMapping
    public ReservationResponse reservationByRecordLocator(@Argument String recordLocator) {
        return queries.getByRecordLocator(recordLocator);
    }

    /** Query.reservationsByLoyaltyNumber  (REST #3) */
    @QueryMapping
    public List<ReservationResponse> reservationsByLoyaltyNumber(@Argument String loyaltyNumber) {
        return queries.getByLoyaltyNumber(loyaltyNumber);
    }
}
```

### Side-by-side with the REST controller

| REST (`ReservationQueryController`) | GraphQL (`ReservationGraphQlController`) | Why it differs |
|---|---|---|
| `@RestController` | `@Controller` | GraphQL doesn't write the HTTP body itself; the GraphQL engine serializes the result |
| `@GetMapping("/{recordLocator}")` | `@QueryMapping` | It binds by **method name** to the `Query` field with the same name |
| `@PathVariable` / `@RequestParam` | `@Argument` | It binds by **parameter name** to the schema argument |
| `queries.getByRecordLocator(...)` | **the same call** | Business logic is reused, not duplicated |

`@QueryMapping` is shorthand for `@SchemaMapping(typeName = "Query")`. If the Java method name ever differs from the schema field, use `@QueryMapping(name = "schemaFieldName")`.

### What happens at runtime

```
POST /graphql  {"query": "{ reservationByRecordLocator(recordLocator:\"JTK7QP\") { status passengers { firstName } } }"}
  │
  ├─ 1. Parse      → is it valid GraphQL syntax?
  ├─ 2. Validate   → does every field and argument exist in the schema? Are the types right?
  │                  (fails here WITHOUT calling any of your code)
  ├─ 3. Execute
  │     ├─ Query.reservationByRecordLocator → YOUR @QueryMapping method → ReservationResponse
  │     ├─ Reservation.status               → default fetcher → response.status()
  │     ├─ Reservation.passengers           → default fetcher → response.passengers()
  │     └─ Passenger.firstName (each)       → default fetcher → passenger.firstName()
  └─ 4. Build JSON containing ONLY the requested fields
```

**✅ Checkpoint 3:** Restart. The schema inspection report should list **no unmapped fields**.

---

## Step 4: Run it and explore with GraphiQL

GraphiQL is an in-browser GraphQL IDE, similar to the Swagger UI this app already has. Enable it in `src/main/resources/application.properties`:

```properties
spring.graphql.graphiql.enabled=true
```

Run `mvn spring-boot:run` and open **http://localhost:8081/graphiql**.

### Queries to try (using the seeded sample data)

**A single object:**

```graphql
query {
  reservationByRecordLocator(recordLocator: "JTK7QP") {
    recordLocator
    status
    passengers { firstName lastName passengerType }
    segments { flightNumber origin destination durationMinutes }
  }
}
```

**A list** (Priya Nair `JM7654321` has 2 reservations: JTM4XR and JTP2NV):

```graphql
query {
  reservationsByLoyaltyNumber(loyaltyNumber: "JM7654321") {
    recordLocator
    segments { flightNumber cabin }
  }
}
```

**Variables** (how real clients send arguments; put the JSON in GraphiQL's *Variables* panel):

```graphql
query GetReservation($pnr: String!) {
  reservationByRecordLocator(recordLocator: $pnr) {
    status
    contactEmail
  }
}
```

```json
{ "pnr": "JTM4XR" }
```

**Both queries in one request** (impossible with a single REST call):

```graphql
query {
  ava: reservationByRecordLocator(recordLocator: "JTK7QP") { status }
  priya: reservationsByLoyaltyNumber(loyaltyNumber: "JM7654321") { recordLocator }
}
```

(`ava:` and `priya:` are **aliases**. They rename fields in the response.)

### It's just HTTP: call it from Postman or curl

```bash
curl -s localhost:8081/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"query($pnr:String!){ reservationByRecordLocator(recordLocator:$pnr){ status } }","variables":{"pnr":"JTK7QP"}}'
```

In Postman, create a `POST http://localhost:8081/graphql` request and use the **GraphQL** body type.

### Experiments (do them, they're the real learning)

| Try this | What you'll learn |
|---|---|
| Remove fields from a query | The response shape follows the request |
| Ask for a field that doesn't exist, e.g. `foo` | Validation rejects the query **before your code runs** |
| Leave out the required argument | What `!` does on an argument |
| Ask for `passengers` with no sub-fields | Object fields need a selection set; only scalars and enums can be leaves |
| Lowercase PNR `"jtk7qp"` | Your service's `normalize()` still applies, because the logic is shared with REST |

**✅ Checkpoint 4:** Both queries return data in GraphiQL **and** via Postman or curl.

---

## Step 5: Error handling (the big mindset change)

Run this:

```graphql
{ reservationByRecordLocator(recordLocator: "NOPE00") { status } }
```

You'll see:

- **HTTP 200**, not 404
- `"data": { "reservationByRecordLocator": null }`
- an `"errors"` array with a vague **`INTERNAL_ERROR`** message

### Why

- `GlobalExceptionHandler` is a **`@RestControllerAdvice`**, which only handles exceptions from REST controllers.
- GraphQL puts errors **in the response body**, because one request can partly succeed. In the alias example from Step 4, `ava` could succeed while `priya` fails. A single HTTP status code can't express that.

### Fix: map your exceptions to GraphQL errors

Create **`src/main/java/com/jetairlines/graphql/GraphQlExceptionHandlers.java`**:

```java
package com.jetairlines.graphql;

import com.jetairlines.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** GraphQL error mapping: the GraphQL counterpart of GlobalExceptionHandler. */
@ControllerAdvice
public class GraphQlExceptionHandlers {

    @GraphQlExceptionHandler
    public GraphQLError notFound(ResourceNotFoundException ex) {
        return GraphQLError.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(ex.getMessage())
                .build();
    }

    @GraphQlExceptionHandler
    public GraphQLError badRequest(IllegalArgumentException ex) {
        return GraphQLError.newError()
                .errorType(ErrorType.BAD_REQUEST)
                .message(ex.getMessage())
                .build();
    }
}
```

Run the query again. Now you get:

```json
{
  "errors": [{
    "message": "Reservation not found: NOPE00",
    "path": ["reservationByRecordLocator"],
    "extensions": { "classification": "NOT_FOUND" }
  }],
  "data": { "reservationByRecordLocator": null }
}
```

| REST (`ApiError`) | GraphQL error |
|---|---|
| `status: 404` | `extensions.classification: NOT_FOUND` |
| `message` | `message` |
| `path` (URL) | `path` (**field** path inside the query) |

**Why this is safe:** the "not found" message is deliberately generic, so it doesn't reveal which PNRs exist. For any exception you *don't* map, Spring keeps the message hidden (`INTERNAL_ERROR`). That's a good default: never leak stack traces to clients.

**✅ Checkpoint 5:** An unknown PNR returns `classification: NOT_FOUND` with your message. An unknown loyalty number returns an **empty list** and no error. Think about why they differ.

---

## Step 6: Automated tests

Create **`src/test/java/com/jetairlines/graphql/ReservationGraphQlTest.java`**:

```java
package com.jetairlines.graphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
// + import for @AutoConfigureHttpGraphQlTester (let your IDE add it; the package moved in Boot 4)

@SpringBootTest
@AutoConfigureHttpGraphQlTester
class ReservationGraphQlTest {

    @Autowired HttpGraphQlTester tester;

    @Test
    void fetchesReservationByRecordLocator() {
        tester.document("""
                { reservationByRecordLocator(recordLocator: "JTK7QP") {
                    status
                    passengers { lastName }
                } }
                """)
              .execute()
              .path("reservationByRecordLocator.status").entity(String.class).isEqualTo("ACTIVE")
              .path("reservationByRecordLocator.passengers[*].lastName").entityList(String.class).hasSize(2);
    }

    @Test
    void fetchesReservationsByLoyaltyNumber() {
        tester.document("""
                { reservationsByLoyaltyNumber(loyaltyNumber: "JM7654321") { recordLocator } }
                """)
              .execute()
              .path("reservationsByLoyaltyNumber[*].recordLocator").entityList(String.class)
              .containsExactlyInAnyOrder("JTM4XR", "JTP2NV");
    }

    @Test
    void unknownRecordLocatorIsNotFound() {
        tester.document("{ reservationByRecordLocator(recordLocator: \"NOPE00\") { status } }")
              .execute()
              .errors()
              .expect(e -> e.getErrorType() == ErrorType.NOT_FOUND)
              .verify()
              .path("reservationByRecordLocator").valueIsNull();
    }
}
```

**Why:**

- `HttpGraphQlTester` sends real GraphQL requests through the full Spring stack (MockMvc underneath), so the schema, controller, service, H2 and seed data all run.
- `.path(...)` uses a JSONPath-like syntax over `data`.
- `.errors().expect(...)` checks the `errors` array. By default the tester **fails a test on any unexpected error**, which is a useful safety net.

Why `InAnyOrder`? The repository sorts by `createdAt DESC`, but the seeder creates both of Priya's bookings within milliseconds of each other, so their order isn't guaranteed. Asserting an exact order would make the test fail at random. When you write tests, only assert what the code actually guarantees.

**✅ Checkpoint 6:** `mvn test` passes, covering both the existing REST tests and the new GraphQL tests.

---

## Final file layout

```
src/main/java/com/jetairlines
├── controller/                    (unchanged) REST
├── service/                       (unchanged) shared business logic
├── dto/                           (unchanged) reused as GraphQL types
└── graphql/                       NEW
    ├── ReservationGraphQlController.java
    └── GraphQlExceptionHandlers.java
src/main/resources
├── application.properties         + spring.graphql.graphiql.enabled=true
└── graphql/schema.graphqls        NEW
src/test/java/com/jetairlines/graphql
└── ReservationGraphQlTest.java    NEW
pom.xml                            + spring-boot-starter-graphql (+ -test)
```

Notice how small the change is: **2 Java classes, 1 schema, 2 dependencies, 1 property.** That's the payoff of keeping logic in the service layer.

---

## Self-check questions

Answer these without looking at the guide. If you can, you understand it rather than having just copied it.

1. Why did `Passenger.firstName` need no Java code?
2. What happens if the schema says `enum CabinClass { ECONOMY PREMIUM BUSINESS FIRST }` but Java has `PREMIUM_ECONOMY`?
3. Why is the reservation query's return type `Reservation` (nullable) while the list query's is `[Reservation!]!`?
4. Why doesn't `GlobalExceptionHandler` handle GraphQL errors?
5. A query asks for a field that doesn't exist. Does `ReservationQueryService` get called? Why or why not?
6. Why is a "not found" response HTTP 200 in GraphQL?
7. How would you expose REST endpoint #2 (PNR + last name)? Write the schema line and the controller method.
8. Which annotation would you use to expose endpoint #15 (cancel)? What's different about it?

<details>
<summary>Answers (try first!)</summary>

1. graphql-java's default property data fetcher calls the record accessor `firstName()`, because the names match.
2. Serializing `PREMIUM_ECONOMY` fails at runtime with an error for that field. Schema enum values must match the Java constants.
3. A single lookup can legitimately find nothing (null plus an error). A list lookup returns an empty list instead, so the list is never null.
4. It's a `@RestControllerAdvice`, which applies only to Spring MVC handler methods. GraphQL uses `@GraphQlExceptionHandler`.
5. No. Validation against the schema happens before execution, so your code never runs.
6. The HTTP request succeeded. Field-level errors travel in the body, because a response can be part success and part failure.
7. `reservationByRecordLocatorAndLastName(recordLocator: String!, lastName: String!): Reservation`, plus a `@QueryMapping` method with two `@Argument` parameters that calls `getByRecordLocatorAndLastName`.
8. `@MutationMapping`, with `type Mutation { cancelReservation(recordLocator: String!): Reservation }`. By convention, mutations change state; queries must not.

</details>

---

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `/graphql` returns 404 | Missing starter dependency, or the schema file isn't in `src/main/resources/graphql/` with the `.graphqls` extension |
| `/graphiql` returns 404 | `spring.graphql.graphiql.enabled=true` is missing |
| Startup report lists an unmapped `Query.x` | The method name doesn't match the schema field (check for typos) |
| An argument arrives as `null` | The `@Argument` parameter name doesn't match the schema argument name |
| `Can't serialize value … Enum` | Schema enum values differ from the Java enum constants |
| `INTERNAL_ERROR` instead of `NOT_FOUND` | `GraphQlExceptionHandlers` isn't picked up: is it in a scanned package, annotated `@ControllerAdvice`, with handler methods annotated `@GraphQlExceptionHandler`? |
| `Validation error … SubselectionRequired` | You asked for an object field without choosing its sub-fields |
| `LazyInitializationException` | You returned a JPA **entity** instead of a DTO. Keep returning the service's DTOs |

---

## What to learn next

Once the 2 endpoints work, these are the natural next topics, roughly in order:

1. **Custom scalars:** add `com.graphql-java:graphql-java-extended-scalars` and use a real `DateTime` instead of `String`.
2. **Mutations and input types:** expose the write endpoints with `@MutationMapping` and `input CreateReservationInput { ... }`.
3. **`@SchemaMapping` for computed fields:** e.g. add `itinerary: ItinerarySummary` on `Reservation`, resolved by a method that takes the parent `ReservationResponse`.
4. **The N+1 problem and `@BatchMapping`:** when nested fields trigger a database query per parent, batch them. (This app avoids it today because the DTOs are built eagerly.)
5. **Security and limits:** query depth and complexity limits, plus authentication via Spring Security on `/graphql`.
6. **Schema-first versus code-first,** and schema evolution: deprecate fields with `@deprecated` instead of versioning URLs.

### References

- Spring for GraphQL reference: https://docs.spring.io/spring-graphql/reference/
- GraphQL official learning site: https://graphql.org/learn/
- graphql-java: https://www.graphql-java.com/documentation/
