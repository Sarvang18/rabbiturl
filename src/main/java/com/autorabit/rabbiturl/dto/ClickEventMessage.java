package com.autorabit.rabbiturl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickEventMessage {

    private String shortCode;

    private String urlId;

    private String ipAddress;

    private String userAgent;

    private String referrer;

    private String clickedAt;
}
