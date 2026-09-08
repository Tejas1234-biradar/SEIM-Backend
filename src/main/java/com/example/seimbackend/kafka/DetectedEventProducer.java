package com.example.seimbackend.kafka;

import com.example.seimbackend.dto.DetectedEventEnvelope;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectedEventProducer {

    private static final Logger log = LoggerFactory.getLogger(DetectedEventProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(DetectedEventEnvelope envelope) {
        String key = envelope.getEventId() != null ? envelope.getEventId().toString() : "";
        kafkaTemplate.send(KafkaTopics.DETECTED_EVENTS, key, envelope);
        log.debug(
                "Published detected event to Kafka: eventId={}, detectionType={}",
                envelope.getEventId(),
                envelope.getDetectionType());
    }
}

