package com.autorabit.rabbiturl.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopReferrerDto {

    private String referrer;

    private long count;
}
