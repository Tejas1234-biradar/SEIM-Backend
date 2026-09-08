package com.example.seimbackend.service;

import com.example.seimbackend.dto.DetectedEventEnvelope;
import com.example.seimbackend.entity.DetectedEventEntity;
import com.example.seimbackend.repository.DetectedEventRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetectedEventService {

    private static final Logger log = LoggerFactory.getLogger(DetectedEventService.class);

    private final DetectedEventRepository repository;

    @Transactional
    public Optional<DetectedEventEntity> save(DetectedEventEnvelope envelope) {
        UUID eventId = envelope.getEventId();
        Map<String, Object> data = envelope.getData() != null ? envelope.getData() : Map.of();

        if (eventId == null && data.get("eventId") != null) {
            try {
                eventId = UUID.fromString(data.get("eventId").toString());
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }

        if (repository.existsByEventId(eventId)) {
            log.debug("Skipping duplicate detected event with eventId={}", eventId);
            return repository.findByEventId(eventId);
        }

        DetectedEventEntity entity = new DetectedEventEntity();
        entity.setEventId(eventId);
        entity.setDetectionType(
                envelope.getDetectionType() != null && !envelope.getDetectionType().isBlank()
                        ? envelope.getDetectionType()
                        : "Epicenter");

        entity.setDetectedAtTime(parseDouble(data.get("detectedAtTime")));

        Object estX = data.containsKey("estimatedX") ? data.get("estimatedX") : data.get("estimatedEpicenterX");
        entity.setEstimatedX(parseDouble(estX));

        Object estZ = data.containsKey("estimatedZ") ? data.get("estimatedZ") : data.get("estimatedEpicenterZ");
        entity.setEstimatedZ(parseDouble(estZ));

        List<String> stations = parseStationIds(data.get("contributingStationIds"));
        entity.setContributingStationIds(stations);

        if (data.containsKey("contributingStationCount")) {
            entity.setContributingStationCount(parseInt(data.get("contributingStationCount")));
        } else {
            entity.setContributingStationCount(stations.size());
        }

        entity.setPWaveSpeedKmS(parseDouble(data.get("pWaveSpeedKmS")));
        entity.setReceivedAt(Instant.now());

        try {
            DetectedEventEntity saved = repository.save(entity);
            return Optional.of(saved);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate detected event detected on insert for eventId={}", eventId);
            return repository.findByEventId(eventId);
        }
    }

    private double parseDouble(Object val) {
        if (val instanceof Number n) {
            return n.doubleValue();
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }

    private int parseInt(Object val) {
        if (val instanceof Number n) {
            return n.intValue();
        }
        if (val instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private List<String> parseStationIds(Object val) {
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of();
    }
}

