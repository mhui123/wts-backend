package com.wts.api.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStockDto {
    private String stockName;
    private String stockSymbol;
    private BigDecimal quantity;
    private BigDecimal sumDivUsd;
    private BigDecimal sumDivKrw;

    public DashboardStockDto(String stockName, BigDecimal quantity) {
        this.stockName = stockName;
        this.quantity = quantity;
    }

    public DashboardStockDto(String stockName, BigDecimal quantity, BigDecimal sumDivKrw, BigDecimal sumDivUsd) {
        this.stockName = stockName;
        this.quantity = quantity;
        this.sumDivKrw = sumDivKrw;
        this.sumDivUsd = sumDivUsd;
    }
}

