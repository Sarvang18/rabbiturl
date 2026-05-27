package com.autorabit.rabbiturl.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom Redis health indicator for interview demonstration.
 * Spring Boot already registers a default Redis health indicator.
 * This custom one activates only if the default is explicitly disabled.
 */
@Component
@ConditionalOnProperty(name = "management.health.redis.enabled", havingValue = "false", matchIfMissing = false)
public class CustomRedisHealthIndicator implements HealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;

    public CustomRedisHealthIndicator(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            String result = redisTemplate.getConnectionFactory().getConnection().ping();
            if ("PONG".equals(result)) {
                return Health.up()
                        .withDetail("server", "Redis")
                        .build();
            }
            return Health.down()
                    .withDetail("error", "Unexpected ping response: " + result)
                    .build();
        } catch (Exception ex) {
            return Health.down()
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
