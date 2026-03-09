package com.wts.summary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wts.summary.enums.Currency;

import java.math.BigDecimal;

public record TradeHistoryJsonRecord(
        @JsonProperty("trade_date") String tradeDate,
        @JsonProperty("trade_type") String tradeType,
        @JsonProperty("symbol_name") String symbolName,
        @JsonProperty("fx_rate") BigDecimal fxRate,
        @JsonProperty("quantity") BigDecimal quantity,
        @JsonProperty("amount_krw") BigDecimal amountKrw,
        @JsonProperty("amount_usd") BigDecimal amountUsd,
        @JsonProperty("price_krw") BigDecimal priceKrw,
        @JsonProperty("price_usd") BigDecimal priceUsd,
        @JsonProperty("fee_krw") BigDecimal feeKrw,
        @JsonProperty("fee_usd") BigDecimal feeUsd,
        @JsonProperty("tax_krw") BigDecimal taxKrw,
        @JsonProperty("tax_usd") BigDecimal taxUsd,
        @JsonProperty("repay_total_krw") BigDecimal repayTotalKrw,
        @JsonProperty("repay_total_usd") BigDecimal repayTotalUsd,
        @JsonProperty("balance_qty") BigDecimal balanceQty,
        @JsonProperty("balance_krw") BigDecimal balanceKrw,
        @JsonProperty("balance_usd") BigDecimal balanceUsd,
        @JsonProperty("isin") String isin,
        @JsonProperty("trade_currency") Currency tradeCurrency
) { }
