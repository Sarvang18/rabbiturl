package com.autorabit.rabbiturl.service;

import com.autorabit.rabbiturl.dto.*;
import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.model.Url;
import com.autorabit.rabbiturl.repository.ClickEventRepository;
import com.autorabit.rabbiturl.repository.UrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    public AnalyticsService(ClickEventRepository clickEventRepository, UrlRepository urlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
    }

    public AnalyticsSummaryResponse getSummary(String shortCode, int days) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        long totalClicks = clickEventRepository.countByShortCode(shortCode);

        List<DailyClickDto> dailyClicks = mapDailyClicks(
                clickEventRepository.findDailyClickCounts(shortCode, since));

        List<DeviceBreakdownDto> deviceBreakdown = mapBreakdown(
                clickEventRepository.findDeviceBreakdown(shortCode), totalClicks);

        List<DeviceBreakdownDto> browserBreakdown = mapBreakdown(
                clickEventRepository.findBrowserBreakdown(shortCode), totalClicks);

        List<DeviceBreakdownDto> countryBreakdown = mapBreakdown(
                clickEventRepository.findCountryBreakdown(shortCode), totalClicks);

        List<TopReferrerDto> topReferrers = mapReferrers(
                clickEventRepository.findTopReferrers(shortCode, PageRequest.of(0, 10)));

        return AnalyticsSummaryResponse.builder()
                .shortCode(shortCode)
                .longUrl(url.getLongUrl())
                .totalClicks(totalClicks)
                .createdAt(url.getCreatedAt())
                .expiresAt(url.getExpiresAt())
                .dailyClicks(dailyClicks)
                .deviceBreakdown(deviceBreakdown)
                .browserBreakdown(browserBreakdown)
                .countryBreakdown(countryBreakdown)
                .topReferrers(topReferrers)
                .build();
    }

    public List<DailyClickDto> getDailyClicks(String shortCode, int days) {
        urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return mapDailyClicks(clickEventRepository.findDailyClickCounts(shortCode, since));
    }

    public List<TopUrlDto> getTopUrls(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = clickEventRepository.findDailyClickCounts("", since);

        // For top URLs, we query all click events and group by shortCode
        // Using a simpler approach: get all URLs and count their clicks
        return urlRepository.findAll().stream()
                .map(url -> {
                    long clicks = clickEventRepository.countByShortCode(url.getShortCode());
                    return TopUrlDto.builder()
                            .shortCode(url.getShortCode())
                            .longUrl(url.getLongUrl())
                            .clickCount(clicks)
                            .build();
                })
                .filter(dto -> dto.getClickCount() > 0)
                .sorted((a, b) -> Long.compare(b.getClickCount(), a.getClickCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<DailyClickDto> mapDailyClicks(List<Object[]> results) {
        return results.stream()
                .map(row -> DailyClickDto.builder()
                        .date(row[0].toString())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    private List<DeviceBreakdownDto> mapBreakdown(List<Object[]> results, long totalClicks) {
        return results.stream()
                .map(row -> {
                    String label = row[0] != null ? row[0].toString() : "Unknown";
                    long count = (Long) row[1];
                    double percentage = totalClicks > 0
                            ? Math.round((count * 1000.0 / totalClicks)) / 10.0
                            : 0.0;
                    return DeviceBreakdownDto.builder()
                            .label(label)
                            .count(count)
                            .percentage(percentage)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<TopReferrerDto> mapReferrers(List<Object[]> results) {
        return results.stream()
                .map(row -> TopReferrerDto.builder()
                        .referrer(row[0].toString())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }
}
