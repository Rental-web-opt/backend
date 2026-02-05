package com.rental.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic bookingTopic() {
        return TopicBuilder.name("booking-events")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name("notifications")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic eventsTopic() {
        return TopicBuilder.name("events")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
