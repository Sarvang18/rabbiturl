package com.autorabit.rabbiturl.service;

import com.autorabit.rabbiturl.dto.AuthResponse;
import com.autorabit.rabbiturl.dto.LoginRequest;
import com.autorabit.rabbiturl.dto.RegisterRequest;
import com.autorabit.rabbiturl.exception.InvalidTokenException;
import com.autorabit.rabbiturl.exception.UserAlreadyExistsException;
import com.autorabit.rabbiturl.model.RefreshToken;
import com.autorabit.rabbiturl.model.User;
import com.autorabit.rabbiturl.repository.RefreshTokenRepository;
import com.autorabit.rabbiturl.repository.UserRepository;
import com.autorabit.rabbiturl.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleUserId)
                .email("test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .role(User.Role.ROLE_USER)
                .isEnabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private UserDetails mockUserDetails() {
        return org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("$2a$12$hashedpassword")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();
    }

    // ======================== register tests ========================

    @Test
    @DisplayName("register — happy path, user saved and tokens returned")
    void register_happyPath() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password1")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(mockUserDetails());
        when(jwtUtil.generateAccessToken(any(UserDetails.class))).thenReturn("access.token.here");
        when(jwtUtil.generateRefreshToken()).thenReturn("raw-refresh-token");
        when(jwtUtil.hashToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(jwtUtil.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token.here");
        assertThat(response.getRefreshToken()).isEqualTo("raw-refresh-token");
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register — duplicate email throws UserAlreadyExistsException")
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password1")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("test@example.com");

        verify(userRepository, never()).save(any());
    }

    // ======================== login tests ========================

    @Test
    @DisplayName("login — happy path, existing tokens revoked, new tokens returned")
    void login_happyPath() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password1")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("Password1", "$2a$12$hashedpassword")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(mockUserDetails());
        when(jwtUtil.generateAccessToken(any(UserDetails.class))).thenReturn("new.access.token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");
        when(jwtUtil.hashToken("new-refresh-token")).thenReturn("hashed-new-refresh");
        when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(jwtUtil.getRefreshTokenExpiryMs()).thenReturn(604800000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).revokeAllByUserId(sampleUserId);
    }

    @Test
    @DisplayName("login — wrong password throws BadCredentialsException")
    void login_wrongPassword_throwsException() {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPass1")
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPass1", "$2a$12$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login — unknown email throws UsernameNotFoundException")
    void login_unknownEmail_throwsException() {
        LoginRequest request = LoginRequest.builder()
                .email("unknown@example.com")
                .password("Password1")
                .build();

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ======================== refreshToken tests ========================

    @Test
    @DisplayName("refreshToken — happy path, old token revoked, new tokens returned")
    void refreshToken_happyPath() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-old-refresh")
                .user(sampleUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(jwtUtil.hashToken("raw-old-refresh")).thenReturn("hashed-old-refresh");
        when(refreshTokenRepository.findByTokenHash("hashed-old-refresh")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(mockUserDetails());
        when(jwtUtil.generateAccessToken(any(UserDetails.class))).thenReturn("rotated.access.token");
        when(jwtUtil.generateRefreshToken()).thenReturn("rotated-refresh-token");
        when(jwtUtil.hashToken("rotated-refresh-token")).thenReturn("hashed-rotated-refresh");
        when(jwtUtil.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(jwtUtil.getRefreshTokenExpiryMs()).thenReturn(604800000L);

        AuthResponse response = authService.refreshToken("raw-old-refresh");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("rotated.access.token");
        assertThat(response.getRefreshToken()).isEqualTo("rotated-refresh-token");
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("refreshToken — revoked token throws InvalidTokenException")
    void refreshToken_revokedToken_throwsException() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-revoked")
                .user(sampleUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(true)
                .build();

        when(jwtUtil.hashToken("raw-revoked")).thenReturn("hashed-revoked");
        when(refreshTokenRepository.findByTokenHash("hashed-revoked")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken("raw-revoked"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("revoked");
    }

    @Test
    @DisplayName("refreshToken — expired token throws InvalidTokenException")
    void refreshToken_expiredToken_throwsException() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-expired")
                .user(sampleUser)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .revoked(false)
                .build();

        when(jwtUtil.hashToken("raw-expired")).thenReturn("hashed-expired");
        when(refreshTokenRepository.findByTokenHash("hashed-expired")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken("raw-expired"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    // ======================== logout tests ========================

    @Test
    @DisplayName("logout — token revoked successfully")
    void logout_happyPath() {
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .tokenHash("hashed-logout")
                .user(sampleUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        when(jwtUtil.hashToken("raw-logout")).thenReturn("hashed-logout");
        when(refreshTokenRepository.findByTokenHash("hashed-logout")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        authService.logout("raw-logout");

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }
}
