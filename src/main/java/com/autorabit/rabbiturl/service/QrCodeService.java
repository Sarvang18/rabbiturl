package com.autorabit.rabbiturl.service;

import com.autorabit.rabbiturl.exception.QrCodeGenerationException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class QrCodeService {

    public byte[] generateQrCode(String url, int width, int height) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }
        if (width < 100 || width > 2000) {
            throw new IllegalArgumentException("QR size must be between 100 and 2000 pixels");
        }
        if (height < 100 || height > 2000) {
            throw new IllegalArgumentException("QR size must be between 100 and 2000 pixels");
        }

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(url, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            log.info("QR code generated successfully for URL: {}", url);
            return outputStream.toByteArray();

        } catch (WriterException | IOException e) {
            throw new QrCodeGenerationException("Failed to generate QR code for URL: " + url);
        }
    }
}
