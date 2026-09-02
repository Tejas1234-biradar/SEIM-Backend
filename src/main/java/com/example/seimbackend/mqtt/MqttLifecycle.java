package com.example.seimbackend.mqtt;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class MqttLifecycle {

    private final MqttSensorBridge mqttSensorBridge;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() throws Exception {
        mqttSensorBridge.start();
    }
}
