package com.example.seimbackend.repository;

import com.example.seimbackend.entity.DetectedEventEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectedEventRepository extends JpaRepository<DetectedEventEntity, UUID> {

    Optional<DetectedEventEntity> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}

