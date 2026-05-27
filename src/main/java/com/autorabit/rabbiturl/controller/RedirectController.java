package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/")
@Tag(name = "Redirect")
public class RedirectController {

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Redirect to original URL")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        log.info("Redirect request for shortCode: {}", shortCode);

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        String longUrl = urlService.resolveUrl(shortCode, ipAddress, userAgent, referrer);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(longUrl));

        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
