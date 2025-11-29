package com.wts.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private BigDecimal feeKrw;
    private BigDecimal feeUsd;
    private BigDecimal taxKrw;
    private BigDecimal taxUsd;
    private BigDecimal repayTotalKrw;
    private BigDecimal repayTotalUsd;
    private BigDecimal balanceQty;
    private BigDecimal balanceKrw;
    private BigDecimal balanceUsd;
    private Integer sourceRow;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

