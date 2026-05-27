package com.autorabit.rabbiturl.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "click_events", indexes = {
        @Index(name = "idx_click_short_code", columnList = "short_code"),
        @Index(name = "idx_click_clicked_at", columnList = "clicked_at"),
        @Index(name = "idx_click_url_id", columnList = "url_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_id", nullable = false)
    private Url url;

    @Column(name = "short_code", nullable = false, length = 20)
    private String shortCode;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(length = 100)
    private String browser;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "clicked_at", nullable = false)
    private LocalDateTime clickedAt;
}
