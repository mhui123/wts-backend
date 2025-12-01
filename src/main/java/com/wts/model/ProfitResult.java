package com.wts.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class ProfitResult {
    private String symbolName;
    private BigDecimal profitKrw = BigDecimal.ZERO;
    private BigDecimal profitUsd = BigDecimal.ZERO;
    private BigDecimal quantity = BigDecimal.ZERO;

    public ProfitResult(String symbolName) {
        this.symbolName = symbolName;
    }

    public void addProfit(BigDecimal krw, BigDecimal usd, BigDecimal qty) {
        this.profitKrw = this.profitKrw.add(krw != null ? krw : BigDecimal.ZERO);
        this.profitUsd = this.profitUsd.add(usd != null ? usd : BigDecimal.ZERO);
        this.quantity = this.quantity.add(qty != null ? qty : BigDecimal.ZERO);

    }
}
