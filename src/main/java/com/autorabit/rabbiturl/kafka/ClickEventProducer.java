package com.autorabit.rabbiturl.kafka;

import com.autorabit.rabbiturl.dto.ClickEventMessage;
import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ClickEventProducer {

    private static final String TOPIC = "click-events";

    private final KafkaTemplate<String, ClickEventMessage> kafkaTemplate;
    private final Counter kafkaPublishFailureCounter;

    public ClickEventProducer(KafkaTemplate<String, ClickEventMessage> kafkaTemplate,
                              Counter kafkaPublishFailureCounter) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaPublishFailureCounter = kafkaPublishFailureCounter;
    }

    public void publishClickEvent(ClickEventMessage message) {
        try {
            kafkaTemplate.send(TOPIC, message.getShortCode(), message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            kafkaPublishFailureCounter.increment();
                            log.error("Failed to publish click event for shortCode: {}", message.getShortCode(), ex);
                        } else {
                            log.debug("Click event published for shortCode: {}, partition: {}, offset: {}",
                                    message.getShortCode(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            kafkaPublishFailureCounter.increment();
            log.error("Kafka producer error for shortCode: {}", message.getShortCode(), e);
        }
    }
}
