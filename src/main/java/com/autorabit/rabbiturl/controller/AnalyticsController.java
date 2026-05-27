package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.dto.AnalyticsSummaryResponse;
import com.autorabit.rabbiturl.dto.DailyClickDto;
import com.autorabit.rabbiturl.dto.TopUrlDto;
import com.autorabit.rabbiturl.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/{shortCode}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get full analytics summary for a short URL")
    public ResponseEntity<?> getSummary(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "30") int days) {

        if (days < 1 || days > 365) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "days must be between 1 and 365"));
        }

        log.info("Analytics summary request for shortCode={}, days={}", shortCode, days);
        AnalyticsSummaryResponse response = analyticsService.getSummary(shortCode, days);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/analytics/{shortCode}/daily")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get daily click time series")
    public ResponseEntity<List<DailyClickDto>> getDailyClicks(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "7") int days) {

        log.info("Daily clicks request for shortCode={}, days={}", shortCode, days);
        List<DailyClickDto> dailyClicks = analyticsService.getDailyClicks(shortCode, days);
        return ResponseEntity.ok(dailyClicks);
    }

    @GetMapping("/admin/analytics/top-urls")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: top clicked URLs")
    public ResponseEntity<List<TopUrlDto>> getTopUrls(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {

        log.info("Admin top URLs request, days={}, limit={}", days, limit);
        List<TopUrlDto> topUrls = analyticsService.getTopUrls(days, limit);
        return ResponseEntity.ok(topUrls);
    }
}
