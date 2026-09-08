package com.example.seimbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "detected_events")
@Getter
@Setter
@NoArgsConstructor
public class DetectedEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private String detectionType;

    @Column(nullable = false)
    private double detectedAtTime;

    @Column(name = "estimated_x", nullable = false)
    private double estimatedX;

    @Column(name = "estimated_z", nullable = false)
    private double estimatedZ;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> contributingStationIds = new ArrayList<>();

    @Column(nullable = false)
    private int contributingStationCount;

    @Column(nullable = false)
    private double pWaveSpeedKmS;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();
}

