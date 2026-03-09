package com.wts.model;

import com.wts.summary.jpa.entity.TradeHistory;
import com.wts.summary.enums.Currency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeHistoryVO(
        String symbolName,
        TradeType tradeType,
        Quantity quantity,
        Money amount,      // 기준 통화 금액
        Money fee,
        Money tax,
        String isin,
        LocalDate tradeDate,
        Long trHistId
) {
    public static TradeHistoryVO from(TradeHistory th) {
        Currency c = th.getIsin().startsWith("US") ? Currency.USD : Currency.KRW;
        return new TradeHistoryVO(
                th.getSymbolName(),
                TradeType.from(th.getTradeType()),
                new Quantity(th.getQuantity()),
                new Money(
                        th.getIsin().startsWith("US") ? getValueOrZero(th.getAmountUsd()) : getValueOrZero(th.getAmountKrw()), c
                ),
                new Money(
                        th.getIsin().startsWith("US") ? getValueOrZero(th.getFeeUsd()) : getValueOrZero(th.getFeeKrw()), c
                ),
                new Money(
                        th.getIsin().startsWith("US") ? getValueOrZero(th.getTaxUsd()) : getValueOrZero(th.getTaxKrw()), c
                ),
                th.getIsin(),
                th.getTradeDate(),
                th.getTrHistId()
        );
    }

    private static BigDecimal getValueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
