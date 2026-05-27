package com.autorabit.rabbiturl.service;

import com.autorabit.rabbiturl.dto.ClickEventMessage;
import com.autorabit.rabbiturl.dto.ShortenRequest;
import com.autorabit.rabbiturl.dto.ShortenResponse;
import com.autorabit.rabbiturl.exception.CustomAliasAlreadyExistsException;
import com.autorabit.rabbiturl.exception.UrlExpiredException;
import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.kafka.ClickEventProducer;
import com.autorabit.rabbiturl.model.Url;
import com.autorabit.rabbiturl.repository.UrlRepository;
import com.autorabit.rabbiturl.util.Base62Encoder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class UrlService {

    private static final String BASE_URL = "rab.it/";
    private static final int MAX_RETRIES = 5;

    private final UrlRepository urlRepository;
    private final RedisService redisService;
    private final ClickEventProducer clickEventProducer;
    private final Counter urlShortenCounter;
    private final Counter urlRedirectCounter;
    private final Counter urlRedirectCacheHitCounter;
    private final Counter urlRedirectCacheMissCounter;
    private final Timer urlShortenTimer;
    private final Timer urlRedirectTimer;

    public UrlService(UrlRepository urlRepository,
                      RedisService redisService,
                      ClickEventProducer clickEventProducer,
                      Counter urlShortenCounter,
                      Counter urlRedirectCounter,
                      Counter urlRedirectCacheHitCounter,
                      Counter urlRedirectCacheMissCounter,
                      Timer urlShortenTimer,
                      Timer urlRedirectTimer) {
        this.urlRepository = urlRepository;
        this.redisService = redisService;
        this.clickEventProducer = clickEventProducer;
        this.urlShortenCounter = urlShortenCounter;
        this.urlRedirectCounter = urlRedirectCounter;
        this.urlRedirectCacheHitCounter = urlRedirectCacheHitCounter;
        this.urlRedirectCacheMissCounter = urlRedirectCacheMissCounter;
        this.urlShortenTimer = urlShortenTimer;
        this.urlRedirectTimer = urlRedirectTimer;
    }

    public ShortenResponse shortenUrl(ShortenRequest request) {
        return urlShortenTimer.record(() -> {
            String shortCode;

            if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
                String alias = request.getCustomAlias();

                if (urlRepository.existsByCustomAlias(alias)) {
                    throw new CustomAliasAlreadyExistsException(alias);
                }

                shortCode = alias;
                log.info("Using custom alias '{}' as short code", alias);
            } else {
                shortCode = generateUniqueShortCode();
                log.info("Generated short code: {}", shortCode);
            }

            Url url = Url.builder()
                    .shortCode(shortCode)
                    .longUrl(request.getLongUrl())
                    .customAlias(request.getCustomAlias())
                    .build();

            if (request.getExpiresInDays() != null) {
                url.setExpiresAt(LocalDateTime.now().plusDays(request.getExpiresInDays()));
            }

            Url savedUrl = urlRepository.save(url);
            log.info("Saved URL with id={} and shortCode={}", savedUrl.getId(), savedUrl.getShortCode());

            // Cache the new URL in Redis
            redisService.cacheUrl(savedUrl.getShortCode(), savedUrl.getLongUrl());

            urlShortenCounter.increment();

            return mapToResponse(savedUrl);
        });
    }

    public String resolveUrl(String shortCode, String ipAddress, String userAgent, String referrer) {
        return urlRedirectTimer.record(() -> {
            String longUrl;
            String urlId;

            // Check Redis cache first
            Optional<String> cached = redisService.getCachedUrl(shortCode);
            if (cached.isPresent()) {
                log.info("Cache HIT for: {}", shortCode);
                urlRedirectCacheHitCounter.increment();
                longUrl = cached.get();

                // We still need the URL ID for the Kafka message
                Url url = urlRepository.findByShortCode(shortCode).orElse(null);
                if (url == null) {
                    urlRedirectCounter.increment();
                    return longUrl;
                }
                urlId = url.getId().toString();
            } else {
                log.info("Cache MISS for: {}", shortCode);
                urlRedirectCacheMissCounter.increment();

                // Fall back to database
                Url url = urlRepository.findByShortCode(shortCode)
                        .orElseThrow(() -> new UrlNotFoundException(shortCode));

                if (!url.isActive()) {
                    throw new UrlExpiredException(shortCode);
                }

                if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(LocalDateTime.now())) {
                    url.setActive(false);
                    urlRepository.save(url);
                    throw new UrlExpiredException(shortCode);
                }

                // Warm the cache after a DB hit
                redisService.cacheUrl(shortCode, url.getLongUrl());

                longUrl = url.getLongUrl();
                urlId = url.getId().toString();
                log.info("Resolved shortCode={} to longUrl={}", shortCode, longUrl);
            }

            // Publish click event to Kafka — fire and forget
            ClickEventMessage message = ClickEventMessage.builder()
                    .shortCode(shortCode)
                    .urlId(urlId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent != null ? userAgent : "Unknown")
                    .referrer(referrer)
                    .clickedAt(LocalDateTime.now().toString())
                    .build();

            clickEventProducer.publishClickEvent(message);

            urlRedirectCounter.increment();
            return longUrl;
        });
    }

    @Transactional(readOnly = true)
    public ShortenResponse getUrlInfo(String shortCode) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        return mapToResponse(url);
    }

    private String generateUniqueShortCode() {
        for (int i = 0; i < MAX_RETRIES; i++) {
            String code = Base62Encoder.generateRandom();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
            log.info("Short code collision on attempt {}, retrying...", i + 1);
        }
        throw new RuntimeException("Could not generate unique short code after 5 retries");
    }

    private ShortenResponse mapToResponse(Url url) {
        return ShortenResponse.builder()
                .shortUrl(BASE_URL + url.getShortCode())
                .shortCode(url.getShortCode())
                .longUrl(url.getLongUrl())
                .customAlias(url.getCustomAlias())
                .expiresAt(url.getExpiresAt())
                .createdAt(url.getCreatedAt())
                .clickCount(url.getClickCount())
                .qrCodeUrl(null)
                .build();
    }
}
