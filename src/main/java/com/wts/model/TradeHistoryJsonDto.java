package com.wts.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeHistoryJsonDto {

    @JsonProperty("trade_date")
    private Long tradeDate;           // timestamp (밀리초)

    @JsonProperty("trade_type")
    private String tradeType;

    @JsonProperty("symbol_name")
    private String symbolName;

    @JsonProperty("fx_rate")
    private BigDecimal fxRate;

    @JsonProperty("quantity")
    private BigDecimal quantity;

    @JsonProperty("amount_krw")
    private BigDecimal amountKrw;

    @JsonProperty("amount_usd")
    private BigDecimal amountUsd;

    @JsonProperty("price_krw")
    private BigDecimal priceKrw;

    @JsonProperty("price_usd")
    private BigDecimal priceUsd;

    @JsonProperty("fee_krw")
    private BigDecimal feeKrw;

    @JsonProperty("fee_usd")
    private BigDecimal feeUsd;

    @JsonProperty("tax_krw")
    private BigDecimal taxKrw;

    @JsonProperty("tax_usd")
    private BigDecimal taxUsd;

    @JsonProperty("repay_total_krw")
    private BigDecimal repayTotalKrw;

    @JsonProperty("repay_total_usd")
    private BigDecimal repayTotalUsd;

    @JsonProperty("balance_qty")
    private BigDecimal balanceQty;

    @JsonProperty("balance_krw")
    private BigDecimal balanceKrw;

    @JsonProperty("balance_usd")
    private BigDecimal balanceUsd;

    @JsonProperty("isin")
    private String isin;
}
