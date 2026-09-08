package com.example.seimbackend.config;

import com.example.seimbackend.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic sensorReadingsTopic() {
        return new NewTopic(KafkaTopics.SENSOR_READINGS, 1, (short) 1);
    }

    @Bean
    public NewTopic detectedEventsTopic() {
        return new NewTopic(KafkaTopics.DETECTED_EVENTS, 1, (short) 1);
    }
}
