package com.wts.model;

import java.math.RoundingMode;

public record PortfolioPositionVO(
        Quantity buyQty,
        Quantity sellQty,
        Quantity holdingQty,
        Money totalBuy,
        Money totalSell,
        Money profit,
        Money divined,
        Money holdingAmount,
        Money holdingPrice
) {
    public Money avgBuyPrice() {
        if (buyQty.isZero()) {
            return Money.zero(totalBuy.currency());
        }
        return new Money(
                totalBuy.amount().divide(buyQty.value(), 8, RoundingMode.HALF_UP),
                totalBuy.currency()
        );
    }

    public Money avgSellPrice() {
        if (sellQty.isZero()) {
            return Money.zero(totalSell.currency());
        }
        return new Money(
                totalSell.amount().divide(sellQty.value(), 8, RoundingMode.HALF_UP),
                totalSell.currency()
        );
    }
}
