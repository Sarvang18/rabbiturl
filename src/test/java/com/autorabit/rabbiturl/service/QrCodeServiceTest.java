package com.autorabit.rabbiturl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class QrCodeServiceTest {

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService();
    }

    @Test
    @DisplayName("generateQrCode — valid URL 300x300 returns non-empty byte array")
    void generateQrCode_validUrl_returnsBytes() {
        byte[] result = qrCodeService.generateQrCode("https://rab.it/test123", 300, 300);

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);
    }

    @Test
    @DisplayName("generateQrCode — blank URL throws IllegalArgumentException")
    void generateQrCode_blankUrl_throwsException() {
        assertThatThrownBy(() -> qrCodeService.generateQrCode("", 300, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("URL must not be blank");
    }

    @Test
    @DisplayName("generateQrCode — width 50 (below minimum) throws IllegalArgumentException")
    void generateQrCode_widthTooSmall_throwsException() {
        assertThatThrownBy(() -> qrCodeService.generateQrCode("https://rab.it/test", 50, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QR size must be between 100 and 2000 pixels");
    }

    @Test
    @DisplayName("generateQrCode — width 3000 (above maximum) throws IllegalArgumentException")
    void generateQrCode_widthTooLarge_throwsException() {
        assertThatThrownBy(() -> qrCodeService.generateQrCode("https://rab.it/test", 3000, 300))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("QR size must be between 100 and 2000 pixels");
    }

    @Test
    @DisplayName("generateQrCode — returned byte array starts with PNG magic bytes")
    void generateQrCode_returnsPng() {
        byte[] result = qrCodeService.generateQrCode("https://rab.it/pngcheck", 300, 300);

        // PNG magic bytes: 0x89, 0x50, 0x4E, 0x47
        assertThat(result[0]).isEqualTo((byte) 0x89);
        assertThat(result[1]).isEqualTo((byte) 0x50);
        assertThat(result[2]).isEqualTo((byte) 0x4E);
        assertThat(result[3]).isEqualTo((byte) 0x47);
    }
}
