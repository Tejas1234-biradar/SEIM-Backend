package com.example.seimbackend.mqtt;

import com.example.seimbackend.dto.DetectedEventEnvelope;
import com.example.seimbackend.dto.SensorReadingEnvelope;
import com.example.seimbackend.kafka.DetectedEventProducer;
import com.example.seimbackend.kafka.SensorReadingProducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MqttSensorBridge {

    private static final Logger log = LoggerFactory.getLogger(MqttSensorBridge.class);

    private final MqttProperties mqttProperties;
    private final SensorReadingProducer sensorReadingProducer;
    private final DetectedEventProducer detectedEventProducer;
    private final ObjectMapper objectMapper;

    private MqttClient mqttClient;

    public void start() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            return;
        }

        mqttClient = new MqttClient(
                mqttProperties.getBrokerUrl(),
                mqttProperties.getClientId() + "-" + UUID.randomUUID(),
                new MemoryPersistence());

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        mqttClient.connect(options);
        mqttClient.subscribe(mqttProperties.getTopic(), sensorMessageListener());
        mqttClient.subscribe(mqttProperties.getDetectionTopic(), detectionMessageListener());
        log.info(
            "MQTT bridge connected to {} and subscribed to {} and {}",
                mqttProperties.getBrokerUrl(),
                mqttProperties.getTopic(),
                mqttProperties.getDetectionTopic());
    }

    public void stop() {
        if (mqttClient == null) {
            return;
        }
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
            mqttClient.close();
        } catch (MqttException ex) {
            log.warn("Error while disconnecting MQTT client", ex);
        }
    }

    private IMqttMessageListener sensorMessageListener() {
        return (topic, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            log.debug("Received MQTT sensor message on topic {}: {}", topic, payload);
            try {
                SensorReadingEnvelope envelope = toEnvelope(topic, payload);
                sensorReadingProducer.publish(envelope);
            } catch (Exception ex) {
                log.error("Failed to process MQTT sensor message on topic {}", topic, ex);
            }
        };
    }

    private IMqttMessageListener detectionMessageListener() {
        return (topic, message) -> {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            log.debug("Received MQTT detection message on topic {}: {}", topic, payload);
            try {
                DetectedEventEnvelope envelope = toDetectionEnvelope(topic, payload);
                detectedEventProducer.publish(envelope);
            } catch (Exception ex) {
                log.error("Failed to process MQTT detection message on topic {}", topic, ex);
            }
        };
    }

    SensorReadingEnvelope toEnvelope(String topic, String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);

        if (root.has("readingType") && root.has("data")) {
            SensorReadingEnvelope envelope = objectMapper.treeToValue(root, SensorReadingEnvelope.class);
            if (envelope.getEventId() == null) {
                envelope.setEventId(UUID.randomUUID());
            }
            return envelope;
        }

        String readingType = extractReadingType(topic);
        SensorReadingEnvelope envelope = new SensorReadingEnvelope();
        envelope.setEventId(UUID.randomUUID());
        envelope.setReadingType(readingType);
        envelope.setStationId(root.path("stationId").asText());
        envelope.setTimestamp(root.path("timestamp").asDouble());
        envelope.setData(objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {}));
        return envelope;
    }

    DetectedEventEnvelope toDetectionEnvelope(String topic, String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);

        if (root.has("detectionType") && root.has("data")) {
            DetectedEventEnvelope envelope = objectMapper.treeToValue(root, DetectedEventEnvelope.class);
            if (envelope.getEventId() == null) {
                envelope.setEventId(UUID.randomUUID());
            }
            return envelope;
        }

        String detectionType = extractDetectionType(topic);
        UUID eventId;
        if (root.has("eventId") && !root.path("eventId").isNull() && !root.path("eventId").asText().isBlank()) {
            try {
                eventId = UUID.fromString(root.path("eventId").asText());
            } catch (IllegalArgumentException e) {
                eventId = UUID.randomUUID();
            }
        } else {
            eventId = UUID.randomUUID();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = objectMapper.convertValue(root, Map.class);
        if (!dataMap.containsKey("eventId")) {
            dataMap.put("eventId", eventId.toString());
        }

        DetectedEventEnvelope envelope = new DetectedEventEnvelope();
        envelope.setEventId(eventId);
        envelope.setDetectionType(detectionType);
        envelope.setData(dataMap);
        return envelope;
    }

    private String extractReadingType(String topic) {
        // Topic pattern: sensors/{readingType}/readings
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "Unknown";
    }

    private String extractDetectionType(String topic) {
        // Topic pattern: detections/{detectionType}/events
        String[] parts = topic.split("/");
        if (parts.length >= 2) {
            return parts[1];
        }
        return "Epicenter";
    }
}
