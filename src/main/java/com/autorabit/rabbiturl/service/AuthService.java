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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       JwtUtil jwtUtil,
                       PasswordEncoder passwordEncoder,
                       UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with email={}", savedUser.getEmail());

        return generateAuthResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("User logged in: email={}", user.getEmail());

        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(String rawRefreshToken) {
        String tokenHash = jwtUtil.hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (storedToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Revoke the used token (rotation)
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        log.info("Refresh token rotated for user: email={}", user.getEmail());

        return generateAuthResponse(user);
    }

    public void logout(String rawRefreshToken) {
        String tokenHash = jwtUtil.hashToken(rawRefreshToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for logout");
        });
    }

    private AuthResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String rawRefreshToken = jwtUtil.generateRefreshToken();
        String refreshTokenHash = jwtUtil.hashToken(rawRefreshToken);

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .tokenHash(refreshTokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshTokenExpiryMs() / 1000))
                .build();

        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiryMs() / 1000)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
