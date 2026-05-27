package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.dto.AuthResponse;
import com.autorabit.rabbiturl.dto.LoginRequest;
import com.autorabit.rabbiturl.dto.RegisterRequest;
import com.autorabit.rabbiturl.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Validated
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request for email={}", request.getEmail());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive tokens")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email={}", request.getEmail());
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and get new access token")
    public ResponseEntity<AuthResponse> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        log.info("Token refresh request received");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token")
    public ResponseEntity<Void> logout(@RequestHeader("X-Refresh-Token") String refreshToken) {
        log.info("Logout request received");
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }
}
