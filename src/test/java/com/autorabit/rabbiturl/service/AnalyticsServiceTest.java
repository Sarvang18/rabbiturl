package com.autorabit.rabbiturl.service;

import com.autorabit.rabbiturl.dto.AnalyticsSummaryResponse;
import com.autorabit.rabbiturl.dto.DailyClickDto;
import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.model.Url;
import com.autorabit.rabbiturl.repository.ClickEventRepository;
import com.autorabit.rabbiturl.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private UrlRepository urlRepository;

    private AnalyticsService analyticsService;

    private Url sampleUrl;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(clickEventRepository, urlRepository);

        sampleUrl = Url.builder()
                .id(UUID.randomUUID())
                .shortCode("abc1234")
                .longUrl("https://www.example.com")
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();
    }

    private List<Object[]> objectArrayList(Object[]... arrays) {
        return Arrays.asList(arrays);
    }

    @Test
    @DisplayName("getSummary — happy path returns populated response")
    void getSummary_happyPath() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));
        when(clickEventRepository.countByShortCode("abc1234")).thenReturn(100L);
        when(clickEventRepository.findDailyClickCounts(eq("abc1234"), any(LocalDateTime.class)))
                .thenReturn(objectArrayList(
                        new Object[]{LocalDate.of(2026, 5, 25), 60L},
                        new Object[]{LocalDate.of(2026, 5, 26), 40L}
                ));
        when(clickEventRepository.findDeviceBreakdown("abc1234"))
                .thenReturn(objectArrayList(
                        new Object[]{"DESKTOP", 70L},
                        new Object[]{"MOBILE", 30L}
                ));
        when(clickEventRepository.findBrowserBreakdown("abc1234"))
                .thenReturn(objectArrayList(new Object[]{"Chrome 124", 80L}));
        when(clickEventRepository.findCountryBreakdown("abc1234"))
                .thenReturn(objectArrayList(new Object[]{"India", 90L}));
        when(clickEventRepository.findTopReferrers(eq("abc1234"), any(PageRequest.class)))
                .thenReturn(objectArrayList(new Object[]{"https://google.com", 50L}));

        AnalyticsSummaryResponse response = analyticsService.getSummary("abc1234", 30);

        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo("abc1234");
        assertThat(response.getTotalClicks()).isEqualTo(100L);
        assertThat(response.getDailyClicks()).hasSize(2);
        assertThat(response.getDeviceBreakdown()).hasSize(2);
        assertThat(response.getBrowserBreakdown()).hasSize(1);
        assertThat(response.getCountryBreakdown()).hasSize(1);
        assertThat(response.getTopReferrers()).hasSize(1);
    }

    @Test
    @DisplayName("getSummary — unknown shortCode throws UrlNotFoundException")
    void getSummary_unknownCode_throwsException() {
        when(urlRepository.findByShortCode("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analyticsService.getSummary("nope", 30))
                .isInstanceOf(UrlNotFoundException.class)
                .hasMessageContaining("nope");
    }

    @Test
    @DisplayName("getSummary — zero total clicks returns 0.0 percentages")
    void getSummary_zeroClicks_noDiv() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));
        when(clickEventRepository.countByShortCode("abc1234")).thenReturn(0L);
        when(clickEventRepository.findDailyClickCounts(eq("abc1234"), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findDeviceBreakdown("abc1234"))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findBrowserBreakdown("abc1234"))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findCountryBreakdown("abc1234"))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findTopReferrers(eq("abc1234"), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        AnalyticsSummaryResponse response = analyticsService.getSummary("abc1234", 30);

        assertThat(response.getTotalClicks()).isEqualTo(0L);
        assertThat(response.getDailyClicks()).isEmpty();
        assertThat(response.getDeviceBreakdown()).isEmpty();
    }

    @Test
    @DisplayName("getDailyClicks — happy path returns correct list size")
    void getDailyClicks_happyPath() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));
        when(clickEventRepository.findDailyClickCounts(eq("abc1234"), any(LocalDateTime.class)))
                .thenReturn(objectArrayList(
                        new Object[]{LocalDate.of(2026, 5, 25), 10L},
                        new Object[]{LocalDate.of(2026, 5, 26), 20L},
                        new Object[]{LocalDate.of(2026, 5, 27), 30L}
                ));

        List<DailyClickDto> result = analyticsService.getDailyClicks("abc1234", 7);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getCount()).isEqualTo(10L);
        assertThat(result.get(2).getCount()).isEqualTo(30L);
    }

    @Test
    @DisplayName("Device breakdown percentage — rounds to 1 decimal correctly")
    void deviceBreakdown_percentageCalculation() {
        when(urlRepository.findByShortCode("abc1234")).thenReturn(Optional.of(sampleUrl));
        when(clickEventRepository.countByShortCode("abc1234")).thenReturn(3L);
        when(clickEventRepository.findDailyClickCounts(eq("abc1234"), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findDeviceBreakdown("abc1234"))
                .thenReturn(objectArrayList(
                        new Object[]{"DESKTOP", 1L},
                        new Object[]{"MOBILE", 2L}
                ));
        when(clickEventRepository.findBrowserBreakdown("abc1234"))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findCountryBreakdown("abc1234"))
                .thenReturn(Collections.emptyList());
        when(clickEventRepository.findTopReferrers(eq("abc1234"), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        AnalyticsSummaryResponse response = analyticsService.getSummary("abc1234", 30);

        // 1/3 * 100 = 33.3%, 2/3 * 100 = 66.7%
        assertThat(response.getDeviceBreakdown()).hasSize(2);
        assertThat(response.getDeviceBreakdown().get(0).getPercentage()).isEqualTo(33.3);
        assertThat(response.getDeviceBreakdown().get(1).getPercentage()).isEqualTo(66.7);
    }
}
