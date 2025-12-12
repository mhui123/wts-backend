package com.wts.model;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItemDto {
    //trade_history
    private Long userId;
    private String symbolName;
    private String ticker;
    private String tradeType;
    private BigDecimal quantity;
    private BigDecimal avgPriceKrw;
    private BigDecimal avgPriceUsd;
    private BigDecimal totalAmountKrw;
    private BigDecimal totalAmountUsd;
    private BigDecimal profitLossKrw;
    private BigDecimal profitLossUsd;
    private BigDecimal dividendKrw;
    private BigDecimal dividendUsd;
    private BigDecimal currentPriceKrw;
    private BigDecimal currentPriceUsd;
    private String companyName;
    private String symbol;
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
    private BigDecimal profitKrw;
    private BigDecimal profitUsd;
    private String isin;

    //portfolio_item
    private BigDecimal buyQty;
    private BigDecimal sellQty;
    private BigDecimal avgSellPriceUsd;
    private BigDecimal avgSellPriceKrw;
    private BigDecimal avgBuyPriceUsd;
    private BigDecimal avgBuyPriceKrw;
    private BigDecimal totalInvestmentKrw;
    private BigDecimal totalInvestmentUsd;
    private BigDecimal totalSellUsd;
    private BigDecimal totalSellKrw;
    private BigDecimal totalBuyUsd;
    private BigDecimal totalBuyKrw;
    private BigDecimal ResetPointBuyUsd;
    private BigDecimal preSellAvgPriceUsd;
    private BigDecimal finalSellQty;

    public PortfolioItemDto(Long userId, String companyName) {
        this.userId = userId;
        this.companyName = companyName;
    }
}

