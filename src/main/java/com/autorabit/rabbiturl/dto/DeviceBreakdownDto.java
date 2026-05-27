package com.autorabit.rabbiturl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceBreakdownDto {

    private String label;

    private long count;

    private double percentage;
}
