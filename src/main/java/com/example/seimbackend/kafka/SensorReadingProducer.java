package com.example.seimbackend.kafka;

import com.example.seimbackend.dto.SensorReadingEnvelope;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SensorReadingProducer {

    private static final Logger log = LoggerFactory.getLogger(SensorReadingProducer.class);

    private final KafkaTemplate<String, SensorReadingEnvelope> kafkaTemplate;

    public void publish(SensorReadingEnvelope envelope) {
        kafkaTemplate.send(KafkaTopics.SENSOR_READINGS, envelope.getStationId(), envelope);
        log.debug(
                "Published sensor reading to Kafka: eventId={}, readingType={}, stationId={}",
                envelope.getEventId(),
                envelope.getReadingType(),
                envelope.getStationId());
    }
}
