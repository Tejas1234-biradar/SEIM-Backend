package com.example.seimbackend.service;

import com.example.seimbackend.dto.SensorReadingEnvelope;
import com.example.seimbackend.entity.SensorReadingEntity;
import com.example.seimbackend.repository.SensorReadingRepository;
import com.example.seimbackend.storage.StorageDownsampler;
import java.time.Instant;
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
public class SensorReadingService {

    private static final Logger log = LoggerFactory.getLogger(SensorReadingService.class);

    private final SensorReadingRepository repository;
    private final StorageDownsampler storageDownsampler;

    @Transactional
    public Optional<SensorReadingEntity> save(SensorReadingEnvelope envelope) {
        if (!storageDownsampler.shouldStore(envelope)) {
            log.trace(
                    "Downsampled reading for {}:{}",
                    envelope.getReadingType(),
                    envelope.getStationId());
            return Optional.empty();
        }

        UUID eventId = envelope.getEventId() != null ? envelope.getEventId() : UUID.randomUUID();

        if (repository.existsByEventId(eventId)) {
            log.debug("Skipping duplicate sensor reading with eventId={}", eventId);
            return repository.findByEventId(eventId);
        }

        SensorReadingEntity entity = new SensorReadingEntity();
        entity.setEventId(eventId);
        entity.setReadingType(envelope.getReadingType());
        entity.setStationId(envelope.getStationId());
        entity.setTimestamp(envelope.getTimestamp());
        entity.setPayload(envelope.getData());
        entity.setReceivedAt(Instant.now());

        try {
            SensorReadingEntity saved = repository.save(entity);
            storageDownsampler.recordStored(envelope);
            return Optional.of(saved);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Duplicate sensor reading detected on insert for eventId={}", eventId);
            return repository.findByEventId(eventId);
        }
    }
}
