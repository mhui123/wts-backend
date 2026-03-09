package com.wts.summary.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeHistoryDto {
    private Long trHistId;
    private Long userId;
    private LocalDate tradeDate;
    private String tradeType;
    private String symbolName;
    private BigDecimal fxRate;
    private BigDecimal quantity;
    private BigDecimal amountKrw;
    private BigDecimal amountUsd;
    private BigDecimal priceKrw;
    private BigDecimal priceUsd;
    private BigDecimal balanceQty;
    private BigDecimal balanceKrw;
    private BigDecimal balanceUsd;
}

