package com.autorabit.rabbiturl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyClickDto {

    private String date;

    private long count;
}
