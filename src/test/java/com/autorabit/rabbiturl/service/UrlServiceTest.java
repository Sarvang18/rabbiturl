package com.autorabit.rabbiturl.service;

import com.autorabit.rabbiturl.dto.ShortenRequest;
import com.autorabit.rabbiturl.dto.ShortenResponse;
import com.autorabit.rabbiturl.exception.CustomAliasAlreadyExistsException;
import com.autorabit.rabbiturl.exception.UrlExpiredException;
import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.kafka.ClickEventProducer;
import com.autorabit.rabbiturl.model.Url;
import com.autorabit.rabbiturl.repository.UrlRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private ClickEventProducer clickEventProducer;

    @Mock
    private Counter urlShortenCounter;

    @Mock
    private Counter urlRedirectCounter;

    @Mock
    private Counter urlRedirectCacheHitCounter;

    @Mock
    private Counter urlRedirectCacheMissCounter;

    @Mock
    private Timer urlShortenTimer;

    @Mock
    private Timer urlRedirectTimer;

    private UrlService urlService;

    private Url sampleUrl;
    private UUID sampleId;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // Make timers pass through to the actual supplier (lenient because not all tests use both)
        lenient().when(urlShortenTimer.record(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        lenient().when(urlRedirectTimer.record(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        urlService = new UrlService(urlRepository, redisService, clickEventProducer,
                urlShortenCounter, urlRedirectCounter,
                urlRedirectCacheHitCounter, urlRedirectCacheMissCounter,
                urlShortenTimer, urlRedirectTimer);

        sampleId = UUID.randomUUID();
        sampleUrl = Url.builder()
                .id(sampleId)
                .shortCode("abc1234")
                .longUrl("https://www.example.com/some/long/path")
                .customAlias(null)
                .createdAt(LocalDateTime.now())
                .expiresAt(null)
                .clickCount(0)
                .isActive(true)
                .build();
    }

    // ======================== shortenUrl tests ========================

    @Test
    @DisplayName("shortenUrl — no alias, happy path")
    void shortenUrl_noAlias_happyPath() {
        ShortenRequest request = ShortenRequest.builder()
                .longUrl("https://www.example.com")
                .build();

        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url saved = invocation.getArgument(0);
            saved.setId(sampleId);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        ShortenResponse response = urlService.shortenUrl(request);

        assertThat(response).isNotNull();
        assertThat(response.getShortUrl()).startsWith("rab.it/");
        assertThat(response.getShortCode()).hasSize(7);
        assertThat(response.getLongUrl()).isEqualTo("https://www.example.com");
        assertThat(response.getCustomAlias()).isNull();
        assertThat(response.getQrCodeUrl()).isNull();
        verify(urlRepository).save(any(Url.class));
        verify(redisService).cacheUrl(anyString(), eq("https://www.example.com"));
        verify(urlShortenCounter).increment();
    }

    @Test
    @DisplayName("shortenUrl — with custom alias, happy path")
    void shortenUrl_withCustomAlias_happyPath() {
        ShortenRequest request = ShortenRequest.builder()
                .longUrl("https://www.example.com")
                .customAlias("myalias")
                .build();

        when(urlRepository.existsByCustomAlias("myalias")).thenReturn(false);
        when(urlRepository.save(any(Url.class))).thenAnswer(invocation -> {
            Url saved = invocation.getArgument(0);
            saved.setId(sampleId);
            saved.setCreatedAt(LocalDateTime.now());
            return saved;
        });

        ShortenResponse response = urlService.shortenUrl(request);

        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo("myalias");
        assertThat(response.getShortUrl()).isEqualTo("rab.it/myalias");
        assertThat(response.getCustomAlias()).isEqualTo("myalias");
        verify(urlRepository, never()).existsByShortCode(anyString());
        verify(redisService).cacheUrl("myalias", "https://www.example.com");
        verify(urlShortenCounter).increment();
    }

    @Test
    @DisplayName("shortenUrl — duplicate alias throws CustomAliasAlreadyExistsException")
    void shortenUrl_duplicateAlias_throwsException() {
        ShortenRequest request = ShortenRequest.builder()
                .longUrl("https://www.example.com")
                .customAlias("taken")
                .build();

        when(urlRepository.existsByCustomAlias("taken")).thenReturn(true);

        assertThatThrownBy(() -> urlService.shortenUrl(request))
                .isInstanceOf(CustomAliasAlreadyExistsException.class)
                .hasMessageContaining("taken");

        verify(urlRepository, never()).save(any());
    }

    // ======================== resolveUrl tests ========================

    @Test
    @DisplayName("resolveUrl — cache hit, returns long URL from Redis and publishes Kafka event")
    void resolveUrl_cacheHit() {
        when(redisService.getCachedUrl("abc1234"))
                .thenReturn(Optional.of("https://www.example.com/some/long/path"));
        when(urlRepository.findByShortCode("abc1234"))
                .thenReturn(Optional.of(sampleUrl));

        String longUrl = urlService.resolveUrl("abc1234", "192.168.1.1", "Mozilla/5.0", null);

        assertThat(longUrl).isEqualTo("https://www.example.com/some/long/path");
        verify(urlRepository, never()).incrementClickCount(any());
        verify(clickEventProducer).publishClickEvent(any());
        verify(urlRedirectCacheHitCounter).increment();
        verify(urlRedirectCounter).increment();
    }

    @Test
    @DisplayName("resolveUrl — cache miss, returns long URL from DB, caches it, and publishes Kafka event")
    void resolveUrl_cacheMiss() {
        when(redisService.getCachedUrl("abc1234")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));

        String longUrl = urlService.resolveUrl("abc1234", "192.168.1.1", "Mozilla/5.0", null);

        assertThat(longUrl).isEqualTo("https://www.example.com/some/long/path");
        verify(redisService).cacheUrl("abc1234", "https://www.example.com/some/long/path");
        verify(clickEventProducer).publishClickEvent(any());
        verify(urlRedirectCacheMissCounter).increment();
        verify(urlRedirectCounter).increment();
    }

    @Test
    @DisplayName("resolveUrl — unknown code throws UrlNotFoundException")
    void resolveUrl_unknownCode_throwsException() {
        when(redisService.getCachedUrl("unknown")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.resolveUrl("unknown", "192.168.1.1", "Mozilla/5.0", null))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("resolveUrl — expired URL throws UrlExpiredException")
    void resolveUrl_expiredUrl_throwsException() {
        sampleUrl.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(redisService.getCachedUrl("abc1234")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));
        when(urlRepository.save(any(Url.class))).thenReturn(sampleUrl);

        assertThatThrownBy(() -> urlService.resolveUrl("abc1234", "192.168.1.1", "Mozilla/5.0", null))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessageContaining("abc1234");

        assertThat(sampleUrl.isActive()).isFalse();
        verify(urlRepository).save(sampleUrl);
    }

    @Test
    @DisplayName("resolveUrl — inactive URL throws UrlExpiredException")
    void resolveUrl_inactiveUrl_throwsException() {
        sampleUrl.setActive(false);

        when(redisService.getCachedUrl("abc1234")).thenReturn(Optional.empty());
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));

        assertThatThrownBy(() -> urlService.resolveUrl("abc1234", "192.168.1.1", "Mozilla/5.0", null))
                .isInstanceOf(UrlExpiredException.class)
                .hasMessageContaining("abc1234");
    }

    // ======================== getUrlInfo tests ========================

    @Test
    @DisplayName("getUrlInfo — happy path")
    void getUrlInfo_happyPath() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));

        ShortenResponse response = urlService.getUrlInfo("abc1234");

        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo("abc1234");
        assertThat(response.getLongUrl()).isEqualTo("https://www.example.com/some/long/path");
        assertThat(response.getShortUrl()).isEqualTo("rab.it/abc1234");
    }

    @Test
    @DisplayName("getUrlInfo — unknown code throws UrlNotFoundException")
    void getUrlInfo_unknownCode_throwsException() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getUrlInfo("nope"))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("nope");
    }
}
