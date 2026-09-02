package com.example.seimbackend.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * When true, persist at most one reading per station + reading type per interval.
     * MQTT and Kafka still receive every message.
     */
    private boolean downsampleEnabled = true;

    /** Minimum milliseconds between persisted readings per station + reading type. */
    private long minIntervalMs = 1000;
}
