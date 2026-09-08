package com.example.seimbackend.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.example.seimbackend.dto.DetectedEventEnvelope;
import com.example.seimbackend.kafka.DetectedEventProducer;
import com.example.seimbackend.kafka.SensorReadingProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MqttSensorBridgeDetectionTest {

    private MqttSensorBridge bridge;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MqttProperties properties = new MqttProperties();
        SensorReadingProducer sensorReadingProducer = mock(SensorReadingProducer.class);
        DetectedEventProducer detectedEventProducer = mock(DetectedEventProducer.class);
        objectMapper = new ObjectMapper();

        bridge = new MqttSensorBridge(properties, sensorReadingProducer, detectedEventProducer, objectMapper);
    }

    @Test
    void shouldParseRawDetectionPayload() throws Exception {
        UUID expectedEventId = UUID.randomUUID();
        String json = """
                {
                  "eventId": "%s",
                  "detectedAtTime": 8.42,
                  "estimatedEpicenterX": 15.02,
                  "estimatedEpicenterZ": 7.98,
                  "contributingStationIds": ["ST_01", "ST_02", "ST_03"],
                  "contributingStationCount": 3,
                  "pWaveSpeedKmS": 6.0
                }
                """.formatted(expectedEventId);

        DetectedEventEnvelope envelope = bridge.toDetectionEnvelope("detections/Epicenter/events", json);

        assertEquals(expectedEventId, envelope.getEventId());
        assertEquals("Epicenter", envelope.getDetectionType());
        assertNotNull(envelope.getData());
        assertEquals(8.42, ((Number) envelope.getData().get("detectedAtTime")).doubleValue());
        assertEquals(15.02, ((Number) envelope.getData().get("estimatedEpicenterX")).doubleValue());
    }

    @Test
    void shouldParseWrappedDetectionEnvelope() throws Exception {
        UUID expectedEventId = UUID.randomUUID();
        String json = """
                {
                  "eventId": "%s",
                  "detectionType": "Epicenter",
                  "data": {
                    "detectedAtTime": 8.42,
                    "estimatedEpicenterX": 15.02
                  }
                }
                """.formatted(expectedEventId);

        DetectedEventEnvelope envelope = bridge.toDetectionEnvelope("detections/Epicenter/events", json);

        assertEquals(expectedEventId, envelope.getEventId());
        assertEquals("Epicenter", envelope.getDetectionType());
        assertNotNull(envelope.getData());
        assertEquals(8.42, ((Number) envelope.getData().get("detectedAtTime")).doubleValue());
    }

    @Test
    void shouldGenerateEventIdWhenMissing() throws Exception {
        String json = """
                {
                  "detectedAtTime": 8.42,
                  "estimatedEpicenterX": 15.02,
                  "estimatedEpicenterZ": 7.98
                }
                """;

        DetectedEventEnvelope envelope = bridge.toDetectionEnvelope("detections/Epicenter/events", json);

        assertNotNull(envelope.getEventId());
        assertEquals("Epicenter", envelope.getDetectionType());
        assertEquals(envelope.getEventId().toString(), envelope.getData().get("eventId"));
    }
}

