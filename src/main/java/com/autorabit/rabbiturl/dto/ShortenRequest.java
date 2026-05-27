package com.autorabit.rabbiturl.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShortenRequest {

    @NotBlank(message = "Must be a valid URL")
    @URL(message = "Must be a valid URL")
    private String longUrl;

    @Size(min = 3, max = 50, message = "Alias must be 3–50 characters")
    private String customAlias;

    @Min(value = 1)
    @Max(value = 365)
    private Integer expiresInDays;
}
