package com.example.seimbackend.storage;

import com.example.seimbackend.dto.SensorReadingEnvelope;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StorageDownsampler {

    private final StorageProperties storageProperties;
    private final ConcurrentHashMap<String, Long> lastStoredAtMs = new ConcurrentHashMap<>();

    public StorageDownsampler(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public boolean shouldStore(SensorReadingEnvelope envelope) {
        if (!storageProperties.isDownsampleEnabled()) {
            return true;
        }

        String key = envelope.getReadingType() + ":" + envelope.getStationId();
        long now = System.currentTimeMillis();
        Long lastStored = lastStoredAtMs.get(key);

        if (lastStored != null && now - lastStored < storageProperties.getMinIntervalMs()) {
            return false;
        }

        return true;
    }

    public void recordStored(SensorReadingEnvelope envelope) {
        if (!storageProperties.isDownsampleEnabled()) {
            return;
        }

        String key = envelope.getReadingType() + ":" + envelope.getStationId();
        lastStoredAtMs.put(key, System.currentTimeMillis());
    }
}
