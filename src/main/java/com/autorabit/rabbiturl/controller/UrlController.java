package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.dto.ShortenRequest;
import com.autorabit.rabbiturl.dto.ShortenResponse;
import com.autorabit.rabbiturl.service.UrlService;
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
@RequestMapping("/api/v1/urls")
@Validated
@Tag(name = "URL Shortener")
public class UrlController {

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping("/shorten")
    @Operation(summary = "Shorten a URL")
    public ResponseEntity<ShortenResponse> shortenUrl(@Valid @RequestBody ShortenRequest request) {
        log.info("Received shorten request for URL: {}", request.getLongUrl());
        ShortenResponse response = urlService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortCode}/info")
    @Operation(summary = "Get URL metadata")
    public ResponseEntity<ShortenResponse> getUrlInfo(@PathVariable String shortCode) {
        log.info("Received info request for shortCode: {}", shortCode);
        ShortenResponse response = urlService.getUrlInfo(shortCode);
        return ResponseEntity.ok(response);
    }
}
