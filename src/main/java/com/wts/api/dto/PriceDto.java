package com.wts.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceDto {
    private LocalDate tradeDate;
    private BigDecimal fxRate;
    private BigDecimal amountKrw;
    private BigDecimal amountUsd;
}

