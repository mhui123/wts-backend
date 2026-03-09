package com.wts.summary.domain.portfolio;

import com.wts.model.Money;
import com.wts.model.Quantity;
import com.wts.summary.enums.Currency;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortfolioItem {

    private Long id;
    private String symbol;
    private Quantity quantity;
    private Money totalBuy;
    private Money totalSell;
    private Money dividend;
    private Money profit;
    private Currency currency;

    public Money investedAmount() {
        return totalBuy.subtract(totalSell);
    }
}
