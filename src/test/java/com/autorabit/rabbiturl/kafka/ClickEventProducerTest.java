package com.autorabit.rabbiturl.kafka;

import com.autorabit.rabbiturl.dto.ClickEventMessage;
import io.micrometer.core.instrument.Counter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClickEventProducerTest {

    @Mock
    private KafkaTemplate<String, ClickEventMessage> kafkaTemplate;

    @Mock
    private Counter kafkaPublishFailureCounter;

    private ClickEventProducer clickEventProducer;

    private ClickEventMessage sampleMessage;

    @BeforeEach
    void setUp() {
        clickEventProducer = new ClickEventProducer(kafkaTemplate, kafkaPublishFailureCounter);

        sampleMessage = ClickEventMessage.builder()
                .shortCode("abc1234")
                .urlId("550e8400-e29b-41d4-a716-446655440000")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .referrer("https://google.com")
                .clickedAt("2026-05-26T12:00:00")
                .build();
    }

    @Test
    @DisplayName("publishClickEvent — happy path sends to correct topic and key")
    void publishClickEvent_happyPath() {
        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<String, ClickEventMessage>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("click-events"), eq("abc1234"), any(ClickEventMessage.class)))
                .thenReturn(future);

        clickEventProducer.publishClickEvent(sampleMessage);

        verify(kafkaTemplate, times(1)).send("click-events", "abc1234", sampleMessage);
    }

    @Test
    @DisplayName("publishClickEvent — Kafka failure does not throw and increments failure counter")
    void publishClickEvent_kafkaFailure_doesNotThrow() {
        when(kafkaTemplate.send(anyString(), anyString(), any(ClickEventMessage.class)))
                .thenThrow(new RuntimeException("Kafka is down"));

        assertThatCode(() -> clickEventProducer.publishClickEvent(sampleMessage))
                .doesNotThrowAnyException();

        verify(kafkaPublishFailureCounter).increment();
    }
}
