package com.example.seimbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.seimbackend.dto.DetectedEventEnvelope;
import com.example.seimbackend.entity.DetectedEventEntity;
import com.example.seimbackend.repository.DetectedEventRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class DetectedEventServiceTest {

    private DetectedEventRepository repository;
    private DetectedEventService service;

    @BeforeEach
    void setUp() {
        repository = mock(DetectedEventRepository.class);
        service = new DetectedEventService(repository);
    }

    @Test
    void shouldSaveDetectedEventCorrectly() {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> data = new HashMap<>();
        data.put("eventId", eventId.toString());
        data.put("detectedAtTime", 8.42);
        data.put("estimatedEpicenterX", 15.02);
        data.put("estimatedEpicenterZ", 7.98);
        data.put("contributingStationIds", List.of("ST_01", "ST_02", "ST_03"));
        data.put("contributingStationCount", 3);
        data.put("pWaveSpeedKmS", 6.0);

        DetectedEventEnvelope envelope = new DetectedEventEnvelope(eventId, "Epicenter", data);

        when(repository.existsByEventId(eventId)).thenReturn(false);
        when(repository.save(any(DetectedEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<DetectedEventEntity> result = service.save(envelope);

        assertTrue(result.isPresent());
        ArgumentCaptor<DetectedEventEntity> captor = ArgumentCaptor.forClass(DetectedEventEntity.class);
        verify(repository).save(captor.capture());

        DetectedEventEntity saved = captor.getValue();
        assertEquals(eventId, saved.getEventId());
        assertEquals("Epicenter", saved.getDetectionType());
        assertEquals(8.42, saved.getDetectedAtTime());
        assertEquals(15.02, saved.getEstimatedX());
        assertEquals(7.98, saved.getEstimatedZ());
        assertEquals(List.of("ST_01", "ST_02", "ST_03"), saved.getContributingStationIds());
        assertEquals(3, saved.getContributingStationCount());
        assertEquals(6.0, saved.getPWaveSpeedKmS());
        assertNotNull(saved.getReceivedAt());
    }

    @Test
    void shouldSkipDuplicateWhenAlreadyExists() {
        UUID eventId = UUID.randomUUID();
        DetectedEventEntity existing = new DetectedEventEntity();
        existing.setEventId(eventId);

        DetectedEventEnvelope envelope = new DetectedEventEnvelope(eventId, "Epicenter", Map.of("eventId", eventId.toString()));

        when(repository.existsByEventId(eventId)).thenReturn(true);
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(existing));

        Optional<DetectedEventEntity> result = service.save(envelope);

        assertTrue(result.isPresent());
        assertEquals(existing, result.get());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldHandleDataIntegrityViolationOnConcurrentDuplicate() {
        UUID eventId = UUID.randomUUID();
        DetectedEventEntity existing = new DetectedEventEntity();
        existing.setEventId(eventId);

        DetectedEventEnvelope envelope = new DetectedEventEnvelope(eventId, "Epicenter", Map.of("eventId", eventId.toString()));

        when(repository.existsByEventId(eventId)).thenReturn(false);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(repository.findByEventId(eventId)).thenReturn(Optional.of(existing));

        Optional<DetectedEventEntity> result = service.save(envelope);

        assertTrue(result.isPresent());
        assertEquals(existing, result.get());
        verify(repository).findByEventId(eventId);
    }
}

