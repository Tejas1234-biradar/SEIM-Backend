# disaster-backend (SEIM-Backend)

Backend for the student disaster-response simulation project. It ingests sensor
telemetry from Unity over MQTT, publishes wrapped events to Kafka, and persists
them in PostgreSQL.

## Architecture

```
Unity / mosquitto_pub  →  MQTT (Mosquitto)  →  Spring MQTT bridge
                                                    ↓
                                              Kafka (sensor.readings)
                                                    ↓
                                         Kafka storage consumer
                                                    ↓
                                              PostgreSQL (JSONB)
```

## Prerequisites

- Docker and Docker Compose
- Java 21 and Maven (only if running the app outside Docker)

## Run everything

```bash
docker compose up --build
```

This starts:

| Service    | Port | Purpose                          |
|------------|------|----------------------------------|
| PostgreSQL | 5434 (host) → 5432 | Persistent sensor reading storage |
| Mosquitto  | 11883 (host) → 1883 | MQTT broker (no auth, local dev)  |
| Kafka      | 9092 | Event stream (KRaft, single node) |
| app        | 8080 | Spring Boot backend               |

Wait until the `app` service logs show the MQTT bridge connected and the Kafka
listener started.

## MQTT topic convention

The bridge subscribes to:

```
sensors/+/readings
```

The middle segment is the **reading type** (`Seismic`, `Gnss`, `DartBuoy`,
`TideGauge`, `OceanBottom`). Example topic:

```
sensors/Seismic/readings
```

The MQTT message body is the raw Unity sensor JSON (or a full Kafka envelope —
see below).

## Kafka event envelope

Every message on `sensor.readings` uses this shape:

```json
{
  "eventId": "uuid",
  "readingType": "Seismic",
  "stationId": "ST_01",
  "timestamp": 12.35,
  "data": { "...": "raw reading payload" }
}
```

The MQTT bridge generates `eventId` when it is missing.

## End-to-end test (Phase 6)

Publish sample readings with `mosquitto_pub` (run from any machine that can reach
port 1883):

```bash
# Seismic
mosquitto_pub -h localhost -p 11883 -t sensors/Seismic/readings -m '{"stationId":"ST_01","timestamp":12.35,"accelerationX_g":0.018,"accelerationY_g":0.025,"accelerationZ_g":0.011}'

# GNSS
mosquitto_pub -h localhost -p 11883 -t sensors/Gnss/readings -m '{"stationId":"ST_02","timestamp":15.0,"latitude":35.68,"longitude":139.76,"altitude_m":12.5}'

# Dart buoy
mosquitto_pub -h localhost -p 11883 -t sensors/DartBuoy/readings -m '{"stationId":"DB_01","timestamp":20.1,"waterColumnHeight_m":4012.3}'

# Tide gauge
mosquitto_pub -h localhost -p 11883 -t sensors/TideGauge/readings -m '{"stationId":"TG_01","timestamp":22.5,"seaLevel_m":1.42}'

# Ocean bottom
mosquitto_pub -h localhost -p 11883 -t sensors/OceanBottom/readings -m '{"stationId":"OB_01","timestamp":30.0,"pressure_kPa":102.4}'
```

Verify rows in PostgreSQL:

```bash
docker compose exec postgres psql -U disaster -d disaster -c \
  "SELECT event_id, reading_type, station_id, timestamp, payload FROM sensor_readings ORDER BY received_at;"
```

You can also watch Kafka messages:

```bash
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic sensor.readings \
  --from-beginning
```

## Manual REST test (Phase 2)

Bypass MQTT/Kafka and POST directly to the storage path:

```bash
curl -s -X POST http://localhost:8080/api/sensor-readings \
  -H 'Content-Type: application/json' \
  -d '{
    "eventId": "550e8400-e29b-41d4-a716-446655440000",
    "readingType": "Seismic",
    "stationId": "ST_01",
    "timestamp": 12.35,
    "data": {
      "stationId": "ST_01",
      "timestamp": 12.35,
      "accelerationX_g": 0.018,
      "accelerationY_g": 0.025,
      "accelerationZ_g": 0.011
    }
  }'
```

## Code map by phase

| Phase | Component | Location |
|-------|-----------|----------|
| 1 | JPA entity + repository | `entity/SensorReadingEntity.java`, `repository/SensorReadingRepository.java` |
| 2 | Manual REST endpoint | `controller/SensorReadingController.java` |
| 3–4 | MQTT → Kafka bridge | `mqtt/MqttSensorBridge.java`, `kafka/SensorReadingProducer.java` |
| 5 | Kafka → PostgreSQL consumer | `kafka/SensorReadingConsumer.java`, `service/SensorReadingService.java` |
| — | Shared envelope DTO | `dto/SensorReadingEnvelope.java` |

Deduplication uses a unique constraint on `eventId`. The service checks for
existing rows first, and catches `DataIntegrityViolationException` as a
backstop for concurrent inserts.

## Run locally (without Docker for the app)

Start infrastructure only:

```bash
docker compose up postgres mosquitto kafka
```

Then:

```bash
./mvnw spring-boot:run
```

## Known simplifications

- **No MQTT authentication** — Mosquitto allows anonymous connections for local
  development only.
- **Single Kafka broker** — KRaft mode with one node; no replication or HA.
- **No dead-letter queue** — Kafka consumer failures are logged and the message
  is skipped; no retry or DLQ topic.
- **Minimal error handling** — try/catch with logging around MQTT and Kafka
  processing; no circuit breakers or backoff.
- **No authentication/authorization** — REST endpoint is open.
- **No frontend/WebSocket or ML consumers** — out of scope for this task.
- **Hibernate `ddl-auto: update`** — schema is auto-managed locally instead of
  Flyway/Liquibase migrations.
- **Check-then-insert dedup** — not fully atomic without `INSERT ... ON CONFLICT
  DO NOTHING`, but the unique constraint prevents duplicate rows.
