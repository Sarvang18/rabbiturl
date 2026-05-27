package com.autorabit.rabbiturl.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom business metrics for Prometheus/Grafana dashboards.
 * These metrics are incremented at the service layer, not the controller layer.
 */
@Configuration
@Slf4j
public class MetricsConfig {

    private final MeterRegistry registry;

    public MetricsConfig(MeterRegistry registry) {
        this.registry = registry;
        log.info("Custom metrics configuration initialized");
    }

    @Bean
    public Counter urlShortenCounter() {
        return Counter.builder("rabbiturl.urls.shortened")
                .description("Total number of URLs shortened")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Counter urlRedirectCounter() {
        return Counter.builder("rabbiturl.urls.redirected")
                .description("Total number of URL redirects served")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Counter urlRedirectCacheHitCounter() {
        return Counter.builder("rabbiturl.cache.hits")
                .description("Total Redis cache hits on redirect")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Counter urlRedirectCacheMissCounter() {
        return Counter.builder("rabbiturl.cache.misses")
                .description("Total Redis cache misses on redirect")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Counter rateLimitHitCounter() {
        return Counter.builder("rabbiturl.ratelimit.hits")
                .description("Total number of rate-limit rejections")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Counter kafkaPublishFailureCounter() {
        return Counter.builder("rabbiturl.kafka.publish.failures")
                .description("Total Kafka publish failures")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Timer urlShortenTimer() {
        return Timer.builder("rabbiturl.urls.shorten.duration")
                .description("Time taken to shorten a URL including DB write")
                .tag("app", "rabbiturl")
                .register(registry);
    }

    @Bean
    public Timer urlRedirectTimer() {
        return Timer.builder("rabbiturl.urls.redirect.duration")
                .description("Time taken to resolve and redirect a short URL")
                .tag("app", "rabbiturl")
                .register(registry);
    }
}
