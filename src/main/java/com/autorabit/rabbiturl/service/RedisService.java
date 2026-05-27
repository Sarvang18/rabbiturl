package com.autorabit.rabbiturl.service;

import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
public class RedisService {

    private static final String URL_CACHE_PREFIX = "url:";
    private static final String RATE_LIMIT_PREFIX = "rate:";
    private static final long URL_CACHE_TTL_MINUTES = 30;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final int RATE_LIMIT_MAX_REQUESTS = 100;

    private final RedisTemplate<String, String> redisTemplate;
    private final Counter rateLimitHitCounter;

    public RedisService(RedisTemplate<String, String> redisTemplate, Counter rateLimitHitCounter) {
        this.redisTemplate = redisTemplate;
        this.rateLimitHitCounter = rateLimitHitCounter;
    }

    public void cacheUrl(String shortCode, String longUrl) {
        try {
            String key = URL_CACHE_PREFIX + shortCode;
            redisTemplate.opsForValue().set(key, longUrl, Duration.ofMinutes(URL_CACHE_TTL_MINUTES));
            log.debug("Cached URL for shortCode={}", shortCode);
        } catch (Exception e) {
            log.warn("Redis cache write failed for shortCode={}: {}", shortCode, e.getMessage());
        }
    }

    public Optional<String> getCachedUrl(String shortCode) {
        try {
            String key = URL_CACHE_PREFIX + shortCode;
            String result = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            log.warn("Redis cache read failed for shortCode={}: {}", shortCode, e.getMessage());
            return Optional.empty();
        }
    }

    public void evictUrl(String shortCode) {
        try {
            String key = URL_CACHE_PREFIX + shortCode;
            redisTemplate.delete(key);
            log.debug("Evicted cache for shortCode={}", shortCode);
        } catch (Exception e) {
            log.warn("Redis cache evict failed for shortCode={}: {}", shortCode, e.getMessage());
        }
    }

    public boolean isRateLimited(String clientIp) {
        try {
            String key = RATE_LIMIT_PREFIX + clientIp;
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(RATE_LIMIT_WINDOW_SECONDS));
            }

            if (count != null && count > RATE_LIMIT_MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                rateLimitHitCounter.increment();
                return true;
            }

            return false;
        } catch (Exception e) {
            log.warn("Redis rate limit check failed for IP={}: {} — failing open", clientIp, e.getMessage());
            return false;
        }
    }
}
