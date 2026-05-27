package com.autorabit.rabbiturl.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryResponse {

    private String shortCode;

    private String longUrl;

    private long totalClicks;

    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private List<DailyClickDto> dailyClicks;

    private List<DeviceBreakdownDto> deviceBreakdown;

    private List<DeviceBreakdownDto> browserBreakdown;

    private List<DeviceBreakdownDto> countryBreakdown;

    private List<TopReferrerDto> topReferrers;
}
