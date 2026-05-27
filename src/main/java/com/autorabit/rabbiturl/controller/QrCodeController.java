package com.autorabit.rabbiturl.controller;

import com.autorabit.rabbiturl.exception.UrlNotFoundException;
import com.autorabit.rabbiturl.model.Url;
import com.autorabit.rabbiturl.repository.UrlRepository;
import com.autorabit.rabbiturl.service.QrCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/qr")
@Tag(name = "QR Code")
public class QrCodeController {

    private final QrCodeService qrCodeService;
    private final UrlRepository urlRepository;

    public QrCodeController(QrCodeService qrCodeService, UrlRepository urlRepository) {
        this.qrCodeService = qrCodeService;
        this.urlRepository = urlRepository;
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Generate QR code for a short URL")
    public ResponseEntity<ByteArrayResource> generateQrCode(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "300") int height) {

        log.info("QR code request for shortCode={}, width={}, height={}", shortCode, width, height);

        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));

        String fullUrl = "https://rab.it/" + shortCode;
        byte[] qrBytes = qrCodeService.generateQrCode(fullUrl, width, height);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new ByteArrayResource(qrBytes));
    }
}
