package com.example.seimbackend.controller;

import com.example.seimbackend.dto.SensorReadingEnvelope;
import com.example.seimbackend.entity.SensorReadingEntity;
import com.example.seimbackend.service.SensorReadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor-readings")
@RequiredArgsConstructor
public class SensorReadingController {

    private final SensorReadingService sensorReadingService;

    @PostMapping
    public ResponseEntity<SensorReadingEntity> create(@RequestBody SensorReadingEnvelope envelope) {
        return sensorReadingService
                .save(envelope)
                .map(entity -> ResponseEntity.status(HttpStatus.CREATED).body(entity))
                .orElse(ResponseEntity.status(HttpStatus.ACCEPTED).build());
    }
}
