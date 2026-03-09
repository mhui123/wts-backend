package com.wts.summary.mapper;

import com.wts.api.dto.StockDistributionDto;
import com.wts.model.Money;
import com.wts.summary.domain.portfolio.Portfolio;
import com.wts.summary.dto.DashboardSummaryDto;
import com.wts.summary.dto.DividendDetailDto;
import com.wts.summary.enums.Currency;
import com.wts.summary.jpa.entity.StockDistribution;
import com.wts.summary.jpa.entity.TradeHistory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class DashboardAssembler {
    public static StockDistributionDto toStockDistributionDto(StockDistribution d) {
        return StockDistributionDto.builder()
                .ticker(d.getTicker())
                .distributionPerShare(d.getDistributionPerShare())
                .rocPct(d.getRocPct())
                .declaredDate(d.getDeclaredDate())
                .recordDate(d.getRecordDate())
                .payableDate(d.getPayableDate())
                .build();
    }

    public static DividendDetailDto toDividendDetailDto(TradeHistory t) {
        return DividendDetailDto.builder()
                .userId(t.getUser() != null ? t.getUser().getId() : null)
                .tradeDate(t.getTradeDate())
                .tradeType(t.getTradeType())
                .symbolName(t.getSymbolName())
                .quantity(t.getQuantity())
                .amountKrw(t.getAmountKrw())
                .amountUsd(t.getAmountUsd())
                .taxKrw(t.getTaxKrw())
                .taxUsd(t.getTaxUsd())
                .build();
    }

    public DashboardSummaryDto toDashboardSummaryDto(Portfolio portfolio) {

        Map<Currency, Money> totalInvestment = portfolio.totalInvestment();
        Map<Currency, Money> totalDividend = portfolio.totalDividend();
        Map<Currency, Money> totalProfit = portfolio.totalProfit();

        return DashboardSummaryDto.builder()
            .totalInvestmentKrw(amountOrZero(totalInvestment.get(Currency.KRW)))
            .totalInvestmentUsd(amountOrZero(totalInvestment.get(Currency.USD)))
            .totalDividendKrw(amountOrZero(totalDividend.get(Currency.KRW)))
            .totalDividendUsd(amountOrZero(totalDividend.get(Currency.USD)))
            .totalProfitKrw(amountOrZero(totalProfit.get(Currency.KRW)))
            .totalProfitUsd(amountOrZero(totalProfit.get(Currency.USD)))
            .build();
    }

    private BigDecimal amountOrZero(Money money) {
        return money != null ? money.amount() : BigDecimal.ZERO;
    }

}
