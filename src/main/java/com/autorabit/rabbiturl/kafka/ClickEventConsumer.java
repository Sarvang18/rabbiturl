package com.autorabit.rabbiturl.kafka;

import com.autorabit.rabbiturl.dto.ClickEventMessage;
import com.autorabit.rabbiturl.model.ClickEvent;
import com.autorabit.rabbiturl.model.Url;
import com.autorabit.rabbiturl.repository.ClickEventRepository;
import com.autorabit.rabbiturl.repository.UrlRepository;
import eu.bitwalker.useragentutils.UserAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ClickEventConsumer {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    public ClickEventConsumer(ClickEventRepository clickEventRepository, UrlRepository urlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
    }

    @KafkaListener(
            topics = "click-events",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ClickEventMessage message) {
        try {
            log.debug("Consuming click event for shortCode: {}", message.getShortCode());

            Optional<Url> urlOptional = urlRepository.findById(UUID.fromString(message.getUrlId()));
            if (urlOptional.isEmpty()) {
                log.warn("URL not found for id={}, shortCode={}. Skipping click event.",
                        message.getUrlId(), message.getShortCode());
                return;
            }

            Url url = urlOptional.get();

            UserAgent ua = UserAgent.parseUserAgentString(message.getUserAgent());

            String deviceType = "UNKNOWN";
            try {
                String deviceName = ua.getOperatingSystem().getDeviceType().getName();
                if (deviceName != null && !deviceName.isBlank()) {
                    deviceType = deviceName.toUpperCase();
                }
            } catch (Exception e) {
                log.debug("Could not parse device type from User-Agent");
            }

            String browser = "Unknown";
            try {
                String browserName = ua.getBrowser().getName();
                if (browserName != null && !browserName.isBlank()) {
                    String majorVersion = ua.getBrowserVersion() != null
                            ? ua.getBrowserVersion().getMajorVersion()
                            : "";
                    browser = browserName + (majorVersion.isEmpty() ? "" : " " + majorVersion);
                }
            } catch (Exception e) {
                log.debug("Could not parse browser from User-Agent");
            }

            String operatingSystem = "Unknown";
            try {
                String osName = ua.getOperatingSystem().getName();
                if (osName != null && !osName.isBlank()) {
                    operatingSystem = osName;
                }
            } catch (Exception e) {
                log.debug("Could not parse OS from User-Agent");
            }

            LocalDateTime clickedAt;
            try {
                clickedAt = LocalDateTime.parse(message.getClickedAt());
            } catch (Exception e) {
                clickedAt = LocalDateTime.now();
                log.debug("Could not parse clickedAt timestamp, using current time");
            }

            ClickEvent clickEvent = ClickEvent.builder()
                    .url(url)
                    .shortCode(message.getShortCode())
                    .ipAddress(message.getIpAddress())
                    .deviceType(deviceType)
                    .browser(browser)
                    .operatingSystem(operatingSystem)
                    .referrer(message.getReferrer())
                    .userAgent(message.getUserAgent())
                    .clickedAt(clickedAt)
                    .country(null)
                    .city(null)
                    .build();

            clickEventRepository.save(clickEvent);
            log.debug("Click event saved for shortCode: {}", message.getShortCode());

        } catch (Exception e) {
            log.error("Failed to consume click event for shortCode: {}", message.getShortCode(), e);
        }
    }
}
