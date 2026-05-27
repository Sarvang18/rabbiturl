package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.dto.AnalyticsSummaryResponse;
import com.autorabit.rabbiturl.dto.TopUrlDto;
import com.autorabit.rabbiturl.exception.GlobalExceptionHandler;
import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.security.JwtAuthFilter;
import com.autorabit.rabbiturl.security.JwtUtil;
import com.autorabit.rabbiturl.security.RateLimitFilter;
import com.autorabit.rabbiturl.security.SecurityConfig;
import com.autorabit.rabbiturl.service.AnalyticsService;
import com.autorabit.rabbiturl.service.RedisService;
import com.autorabit.rabbiturl.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class})
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private RedisService redisService;

    @Test
    @DisplayName("GET /api/v1/analytics/{shortCode} authenticated → 200")
    @WithMockUser
    void getSummary_authenticated_returns200() throws Exception {
        AnalyticsSummaryResponse response = AnalyticsSummaryResponse.builder()
                .shortCode("abc1234")
                .longUrl("https://www.example.com")
                .totalClicks(42)
                .build();

        when(analyticsService.getSummary(eq("abc1234"), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/v1/analytics/abc1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.totalClicks").value(42));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/{shortCode} unauthenticated → 403")
    void getSummary_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/abc1234"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/analytics/{shortCode}?days=400 → 400")
    @WithMockUser
    void getSummary_invalidDays_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/abc1234").param("days", "400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("days must be between 1 and 365"));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/{shortCode} unknown shortCode → 404")
    @WithMockUser
    void getSummary_unknownCode_returns404() throws Exception {
        when(analyticsService.getSummary(eq("nope"), anyInt()))
                .thenThrow(new UrlNotFoundException("nope"));

        mockMvc.perform(get("/api/v1/analytics/nope"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/admin/analytics/top-urls with ROLE_USER → 403")
    @WithMockUser(roles = "USER")
    void topUrls_roleUser_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/top-urls"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/admin/analytics/top-urls with ROLE_ADMIN → 200")
    @WithMockUser(roles = "ADMIN")
    void topUrls_roleAdmin_returns200() throws Exception {
        when(analyticsService.getTopUrls(anyInt(), anyInt()))
                .thenReturn(List.of(TopUrlDto.builder()
                        .shortCode("abc1234")
                        .longUrl("https://www.example.com")
                        .clickCount(100)
                        .build()));

        mockMvc.perform(get("/api/v1/admin/analytics/top-urls"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].shortCode").value("abc1234"));
    }
}
