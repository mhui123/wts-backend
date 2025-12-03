package com.wts.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItemDto {

    private Long userId;
    private String symbolName;
    private String ticker;
    private String tradeType;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal avgPriceKrw = BigDecimal.ZERO;
    private BigDecimal avgPriceUsd = BigDecimal.ZERO;
    private BigDecimal totalAmountKrw = BigDecimal.ZERO;
    private BigDecimal totalAmountUsd = BigDecimal.ZERO;
    private BigDecimal profitLossKrw = BigDecimal.ZERO;
    private BigDecimal profitLossUsd = BigDecimal.ZERO;
    private BigDecimal dividendKrw = BigDecimal.ZERO;
    private BigDecimal dividendUsd = BigDecimal.ZERO;
    private BigDecimal currentPriceKrw = BigDecimal.ZERO;
    private BigDecimal currentPriceUsd = BigDecimal.ZERO;
    private String companyName;
    private String symbol;
    private BigDecimal totalValueUsd;
    private BigDecimal totalValueKrw;
    private BigDecimal profitRateUsd;
    private BigDecimal profitRateKrw;
    private String sector;
    private BigDecimal weight;
    private LocalDate tradeDate;
    private BigDecimal amountKrw;
    private BigDecimal amountUsd;
    private BigDecimal priceKrw;
    private BigDecimal priceUsd;
    private BigDecimal feeKrw;
    private BigDecimal feeUsd;
    private BigDecimal taxKrw;
    private BigDecimal taxUsd;
    private BigDecimal profitKrw = BigDecimal.ZERO;
    private BigDecimal profitUsd = BigDecimal.ZERO;

    public PortfolioItemDto(Long userId, String companyName) {
        this.userId = userId;
        this.companyName = companyName;
    }
}

