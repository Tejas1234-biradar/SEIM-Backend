# Agent Task: Implement `disaster-backend` (Java / Spring Boot)

## Project context

This is the backend repository for a student disaster-response simulation
project (earthquake + tsunami detection). It is one of three repositories:

1. `disaster-simulation` (Unity/C#) — sensor simulation, NOT part of this task
2. **`disaster-backend`** (this repo) — ingests sensor telemetry, distributes
   it via an event stream, persists it, and will later feed a dashboard and
   an ML service
3. `disaster-ml` — future ML training/inference, NOT part of this task

Unity publishes sensor readings over MQTT. This backend bridges MQTT into
Kafka, and Kafka fans out to independent consumers (storage now; frontend
and ML are future work, explicitly out of scope for this task).

## Tech stack (use exactly this, do not substitute)

- **Java 21**, **Spring Boot 3.x**
- **Spring for Apache Kafka** (`spring-kafka`)
- **Eclipse Paho MQTT client** (or Spring Integration MQTT) for the MQTT
  bridge into Kafka
- **PostgreSQL** with **Spring Data JPA**, using a **JSONB** column for the
  raw sensor payload (do not fully normalize sensor payload fields into
  separate typed columns — the payload shape varies by reading type)
- **Docker Compose** to run Mosquitto, Kafka (KRaft mode — no separate
  Zookeeper container), and PostgreSQL locally, plus this Spring Boot app
  as a fourth service
- **Maven** (not Gradle) for the build

## Sensor data contract — DO NOT MODIFY

The Unity side already defines these reading types and will not change
them. Readings are intentionally "dumb" observations — no derived fields
(no epicenter, magnitude, arrival times, or severity in the raw payload).

Reading types: `Seismic`, `Gnss`, `DartBuoy`, `TideGauge`, `OceanBottom`.

Example `SeismicReading` payload:
```json
{
  "stationId": "ST_01",
  "timestamp": 12.35,
  "accelerationX_g": 0.018,
  "accelerationY_g": 0.025,
  "accelerationZ_g": 0.011
}
```

## Kafka event envelope

Every message on the `sensor.readings` Kafka topic must be wrapped in this
envelope (the MQTT bridge is responsible for adding `eventId` if Unity's
MQTT payload doesn't already include one — generate a UUID at the bridge if
missing):

```json
{
  "eventId": "uuid",
  "readingType": "Seismic",
  "stationId": "ST_01",
  "timestamp": 12.35,
  "data": { "...": "raw reading payload as sent by Unity" }
}
```

`eventId` exists for deduplication and tracing — the storage consumer should
use it to avoid inserting the same reading twice on redelivery.

## Build order — STRICT, vertical slices only

Build and verify each phase before starting the next. Do not write code for
a later phase while an earlier one is unverified. After each phase, state
clearly how you verified it (command run, output observed) before moving on.

### Phase 1 — Database + entity, no networking yet
- `docker-compose.yml` with a `postgres` service (persist data to a named
  volume so it survives container restarts)
- One JPA entity, `SensorReadingEntity`: `id` (UUID, PK), `eventId` (UUID,
  unique constraint — this is your dedup mechanism), `readingType` (String),
  `stationId` (String), `timestamp` (double), `payload` (JSONB), `receivedAt`
  (timestamp, default now)
- A `SensorReadingRepository` (Spring Data JPA)
- **Verify:** write a throwaway `CommandLineRunner` or a temporary test that
  inserts one row and reads it back via the repository. Show the query
  result. Delete the throwaway code once verified.

### Phase 2 — Minimal REST endpoint (manual testing aid, not the real ingestion path)
- One `POST /api/sensor-readings` controller endpoint accepting the Kafka
  envelope JSON shape directly, saving it via the repository
- This exists purely so you (the agent) and the developer can manually
  `curl` a test payload in and confirm the DB path works, independent of
  MQTT/Kafka being wired up yet
- **Verify:** a `curl` command posting a sample envelope, followed by a
  query showing the row landed in Postgres

### Phase 3 — MQTT (Mosquitto) bridge, logging only
- Add a `mosquitto` service to `docker-compose.yml` (basic config, no auth
  needed for local dev — note this plainly as a local-only simplification)
- Spring MQTT consumer subscribing to a topic (e.g. `sensors/+/readings` —
  confirm/document the exact topic pattern you choose) that, for now, just
  **logs** the received message — no Kafka or DB involvement yet
- **Verify:** use `mosquitto_pub` (document the exact CLI command) to
  publish a sample sensor JSON payload to the topic, show the Spring Boot
  console log receiving it

### Phase 4 — MQTT → Kafka bridge
- Add a `kafka` service to `docker-compose.yml`, KRaft mode, single broker
  (no multi-broker cluster needed for local dev)
- Extend the Phase 3 MQTT consumer: instead of just logging, wrap the
  message in the Kafka envelope (generating `eventId` if absent) and
  publish it to the `sensor.readings` Kafka topic via a Spring Kafka
  producer
- **Verify:** publish via `mosquitto_pub` again, and this time show the
  message landing on the Kafka topic — either via Spring Boot logs from a
  temporary consumer, or via `kafka-console-consumer` inside the Kafka
  container

### Phase 5 — Kafka → PostgreSQL storage consumer
- A `@KafkaListener` consuming `sensor.readings`, deduplicating on
  `eventId` (check-then-insert, or rely on the unique constraint from
  Phase 1 and catch/ignore the constraint violation — pick one, document
  which and why), persisting via the Phase 1 repository
- **Verify:** full end-to-end — `mosquitto_pub` a sample payload, then
  query Postgres directly (`psql` or a repository query) and show the row
  present, with `eventId` matching what was published

### Phase 6 — End-to-end proof
- Publish 3-5 different sample readings (mix of reading types) via
  `mosquitto_pub`, in sequence
- Query Postgres, show all of them landed correctly, with correct
  `readingType`/`stationId`/`payload` values matching what was sent
- This is the actual definition of done for this task — the full chain
  `MQTT → Kafka → PostgreSQL` proven working, end to end, from outside the
  system (no test code, real pub/sub commands)

## Explicitly OUT OF SCOPE for this task — do not build these
- Frontend/WebSocket consumer or any dashboard-facing endpoint beyond the
  Phase 2 manual-testing POST route
- ML consumer or anything under `ml.predictions`
- Authentication/authorization of any kind
- Production-grade error handling, retries, or dead-letter queues (a
  reasonable try/catch with a log statement is enough — note where you've
  simplified and why)
- Kubernetes, cloud deployment, or anything beyond local Docker Compose

## Deliverables checklist
- [ ] `docker-compose.yml` bringing up Postgres, Mosquitto, Kafka, and the
  Spring Boot app together with one command (`docker-compose up`)
- [ ] Clear `README.md` documenting: how to run everything, the exact
  `mosquitto_pub` command to test end-to-end, and which phase each part
  of the code corresponds to
- [ ] A short section in the README titled "Known simplifications" listing
  every place you deliberately cut a corner per the out-of-scope list
  above, so it's clear what's intentional vs. what's missing