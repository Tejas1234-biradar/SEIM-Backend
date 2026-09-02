package com.example.seimbackend.config;

import com.example.seimbackend.mqtt.MqttProperties;
import com.example.seimbackend.storage.StorageProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MqttProperties.class, StorageProperties.class})
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
