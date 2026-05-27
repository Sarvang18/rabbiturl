package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.dto.ShortenRequest;
import com.autorabit.rabbiturl.dto.ShortenResponse;
import com.autorabit.rabbiturl.exception.GlobalExceptionHandler;
import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.security.JwtAuthFilter;
import com.autorabit.rabbiturl.security.JwtUtil;
import com.autorabit.rabbiturl.security.RateLimitFilter;
import com.autorabit.rabbiturl.security.SecurityConfig;
import com.autorabit.rabbiturl.service.RedisService;
import com.autorabit.rabbiturl.service.UrlService;
import com.autorabit.rabbiturl.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class})
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/urls/shorten — valid body returns 201")
    @WithMockUser
    void shortenUrl_validBody_returns201() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .longUrl("https://www.example.com")
                .build();

        ShortenResponse response = ShortenResponse.builder()
                .shortUrl("rab.it/abc1234")
                .shortCode("abc1234")
                .longUrl("https://www.example.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(urlService.shortenUrl(any(ShortenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value("rab.it/abc1234"))
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.longUrl").value("https://www.example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/urls/shorten — blank longUrl returns 400")
    @WithMockUser
    void shortenUrl_blankUrl_returns400() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .longUrl("")
                .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/urls/{shortCode}/info — valid code returns 200")
    @WithMockUser
    void getUrlInfo_validCode_returns200() throws Exception {
        ShortenResponse response = ShortenResponse.builder()
                .shortUrl("rab.it/abc1234")
                .shortCode("abc1234")
                .longUrl("https://www.example.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(urlService.getUrlInfo("abc1234")).thenReturn(response);

        mockMvc.perform(get("/api/v1/urls/abc1234/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.longUrl").value("https://www.example.com"));
    }

    @Test
    @DisplayName("GET /api/v1/urls/{shortCode}/info — unknown code returns 404")
    @WithMockUser
    void getUrlInfo_unknownCode_returns404() throws Exception {
        when(urlService.getUrlInfo("nope")).thenThrow(new UrlNotFoundException("nope"));

        mockMvc.perform(get("/api/v1/urls/nope/info"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No URL found for short code: nope"));
    }

    @Test
    @DisplayName("POST /api/v1/urls/shorten — unauthenticated returns 403")
    void shortenUrl_unauthenticated_returns403() throws Exception {
        ShortenRequest request = ShortenRequest.builder()
                .longUrl("https://www.example.com")
                .build();

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
