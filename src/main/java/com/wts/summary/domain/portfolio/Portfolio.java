package com.wts.summary.domain.portfolio;

import com.wts.summary.enums.Currency;
import com.wts.model.Money;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@AllArgsConstructor
public class Portfolio {
    private List<PortfolioItem> items;

    public Map<Currency, Money> totalInvestment() {
        Map<Currency, Money> result = new HashMap<>();
        for(PortfolioItem item : items) {
            Currency currency = item.getCurrency();
            Money invested = item.investedAmount();
            result.merge(currency, invested, Money::add);
        }
        return result;
    }

    public Map<Currency, Money> totalProfit() {
        Map<Currency, Money> result = new HashMap<>();
        for(PortfolioItem item : items) {
            Currency currency = item.getCurrency();
            Money invested = item.getProfit();
            result.merge(currency, invested, Money::add);
        }
        return result;
    }

    public Map<Currency, Money> totalDividend() {
        Map<Currency, Money> result = new HashMap<>();
        for(PortfolioItem item : items) {
            Currency currency = item.getCurrency();
            Money invested = item.getDividend();
            result.merge(currency, invested, Money::add);
        }
        return result;
    }
}
