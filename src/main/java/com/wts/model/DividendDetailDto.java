package com.wts.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DividendDetailDto {
    private Long userId;
    private LocalDate tradeDate;
    private String tradeType;
    private String symbolName;
    private BigDecimal quantity;
    private BigDecimal amountKrw;
    private BigDecimal amountUsd;
    private BigDecimal taxKrw;
    private BigDecimal taxUsd;
}

