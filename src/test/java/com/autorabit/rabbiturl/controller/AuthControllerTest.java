package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.dto.AuthResponse;
import com.autorabit.rabbiturl.dto.LoginRequest;
import com.autorabit.rabbiturl.dto.RegisterRequest;
import com.autorabit.rabbiturl.exception.GlobalExceptionHandler;
import com.autorabit.rabbiturl.exception.UserAlreadyExistsException;
import com.autorabit.rabbiturl.security.JwtAuthFilter;
import com.autorabit.rabbiturl.security.JwtUtil;
import com.autorabit.rabbiturl.security.RateLimitFilter;
import com.autorabit.rabbiturl.security.SecurityConfig;
import com.autorabit.rabbiturl.service.AuthService;
import com.autorabit.rabbiturl.service.RedisService;
import com.autorabit.rabbiturl.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, JwtAuthFilter.class, RateLimitFilter.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private RedisService redisService;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthResponse sampleAuthResponse() {
        return AuthResponse.builder()
                .accessToken("test.access.token")
                .refreshToken("test-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
                .email("test@example.com")
                .role("ROLE_USER")
                .build();
    }

    // ======================== register tests ========================

    @Test
    @DisplayName("POST /api/v1/auth/register — valid body returns 201")
    void register_validBody_returns201() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password1")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("test.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — invalid email returns 400")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("Password1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — short password returns 400")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Ab1")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register — duplicate email returns 409")
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password1")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("test@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ======================== login tests ========================

    @Test
    @DisplayName("POST /api/v1/auth/login — valid body returns 200")
    void login_validBody_returns200() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password1")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test.access.token"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    // ======================== refresh tests ========================

    @Test
    @DisplayName("POST /api/v1/auth/refresh — valid header returns 200")
    void refresh_validHeader_returns200() throws Exception {
        when(authService.refreshToken(anyString())).thenReturn(sampleAuthResponse());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Refresh-Token", "some-refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test.access.token"));
    }

    // ======================== logout tests ========================

    @Test
    @DisplayName("POST /api/v1/auth/logout — valid header returns 204")
    void logout_validHeader_returns204() throws Exception {
        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("X-Refresh-Token", "some-refresh-token"))
                .andExpect(status().isNoContent());
    }
}
