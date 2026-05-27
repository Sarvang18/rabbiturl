package com.autorabit.rabbiturl.actuator;

import com.autorabit.rabbiturl.dto.ClickEventMessage;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Custom health indicator for Kafka broker connectivity.
 * Reports UP when the click-events topic is reachable with partitions.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    public KafkaHealthIndicator(KafkaTemplate<String, ClickEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public Health health() {
        try (Producer<String, ClickEventMessage> producer =
                     kafkaTemplate.getProducerFactory().createProducer()) {
            List<?> partitions = producer.partitionsFor("click-events");
            if (partitions != null && !partitions.isEmpty()) {
                return Health.up()
                        .withDetail("topic", "click-events")
                        .withDetail("partitions", partitions.size())
                        .build();
            }
            return Health.down()
                    .withDetail("error", "No partitions found for topic click-events")
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
