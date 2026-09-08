package com.example.seimbackend.kafka;

import com.example.seimbackend.dto.DetectedEventEnvelope;
import com.example.seimbackend.service.DetectedEventService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DetectedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DetectedEventConsumer.class);

    private final DetectedEventService detectedEventService;

    @KafkaListener(
            topics = KafkaTopics.DETECTED_EVENTS,
            groupId = "detected-event-storage",
            properties = {
                "spring.json.value.default.type=com.example.seimbackend.dto.DetectedEventEnvelope"
            })
    public void consume(DetectedEventEnvelope envelope) {
        try {
            detectedEventService
                    .save(envelope)
                    .ifPresent(saved -> log.debug(
                            "Stored detected event: eventId={}, detectionType={}",
                            saved.getEventId(),
                            saved.getDetectionType()));
        } catch (Exception ex) {
            log.error("Failed to store detected event eventId={}", envelope.getEventId(), ex);
        }
    }
}

