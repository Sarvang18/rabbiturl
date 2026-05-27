package com.autorabit.rabbiturl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopUrlDto {

    private String shortCode;

    private String longUrl;

    private long clickCount;
}
