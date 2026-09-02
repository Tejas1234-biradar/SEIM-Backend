package com.example.seimbackend.kafka;

import com.example.seimbackend.dto.SensorReadingEnvelope;
import com.example.seimbackend.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensorReadingConsumer {

    private static final Logger log = LoggerFactory.getLogger(SensorReadingConsumer.class);

    private final SensorReadingService sensorReadingService;

    @KafkaListener(topics = KafkaTopics.SENSOR_READINGS, groupId = "sensor-storage")
    public void consume(SensorReadingEnvelope envelope) {
        try {
            sensorReadingService
                    .save(envelope)
                    .ifPresent(saved -> log.debug(
                            "Stored sensor reading: eventId={}, readingType={}, stationId={}",
                            saved.getEventId(),
                            saved.getReadingType(),
                            saved.getStationId()));
        } catch (Exception ex) {
            log.error("Failed to store sensor reading eventId={}", envelope.getEventId(), ex);
        }
    }
}
