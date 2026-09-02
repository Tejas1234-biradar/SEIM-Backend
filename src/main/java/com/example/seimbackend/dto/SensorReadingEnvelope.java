package com.example.seimbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SensorReadingEnvelope {

    private UUID eventId;
    private String readingType;
    private String stationId;
    private double timestamp;
    private Map<String, Object> data;
}
