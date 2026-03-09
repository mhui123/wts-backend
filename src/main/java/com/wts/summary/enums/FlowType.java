package com.wts.summary.enums;

import lombok.Getter;

import static com.wts.summary.enums.FlowCategory.CASH;
import static com.wts.summary.enums.FlowCategory.FX;
import static com.wts.summary.enums.FlowCategory.STOCK;
import static com.wts.summary.enums.InOut.IN;
import static com.wts.summary.enums.InOut.OUT;


@Getter
public enum FlowType {

    DEPOSIT(IN, CASH),
    WITHDRAW(OUT, CASH),
    BUY(OUT, CASH),
    SELL(IN, CASH),
    DIVIDEND(IN, CASH),
    DIVIDEND_CANCEL(IN, CASH),
    FEE(OUT, CASH),
    TAX_ACCRUED(OUT, CASH),
    TAX_PAID(OUT, CASH),
    FX_GAIN(IN, FX),
    FX_LOSS(OUT, FX),
    STOCK_REWARD(IN, STOCK),
    INTEREST(IN, CASH);

    private final InOut inOut;
    private final FlowCategory category;

    FlowType(InOut inOut, FlowCategory category) {
        this.inOut = inOut;
        this.category = category;
    }
}
