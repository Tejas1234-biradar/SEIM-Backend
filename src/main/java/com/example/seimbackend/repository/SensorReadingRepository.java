package com.example.seimbackend.repository;

import com.example.seimbackend.entity.SensorReadingEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorReadingRepository extends JpaRepository<SensorReadingEntity, UUID> {

    Optional<SensorReadingEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
