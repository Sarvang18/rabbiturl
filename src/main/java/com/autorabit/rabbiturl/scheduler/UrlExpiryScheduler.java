package com.autorabit.rabbiturl.scheduler;

import com.autorabit.rabbiturl.repository.ClickEventRepository;
import com.autorabit.rabbiturl.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class UrlExpiryScheduler {

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;

    public UrlExpiryScheduler(UrlRepository urlRepository, ClickEventRepository clickEventRepository) {
        this.urlRepository = urlRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deactivateExpiredUrls() {
        try {
            int count = urlRepository.deactivateExpiredUrls(LocalDateTime.now());
            log.info("Expired URL deactivation job completed. {} URLs deactivated.", count);
        } catch (Exception e) {
            log.error("Failed to run expired URL deactivation job", e);
        }
    }

    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void deleteOldClickEvents() {
        try {
            int count = clickEventRepository.deleteOlderThan(LocalDateTime.now().minusDays(90));
            log.info("Old click event cleanup completed. {} records deleted.", count);
        } catch (Exception e) {
            log.error("Failed to run old click event cleanup job", e);
        }
    }
}
