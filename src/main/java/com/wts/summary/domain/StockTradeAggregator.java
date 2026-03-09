package com.wts.summary.domain;

import com.wts.model.PortfolioPositionVO;
import com.wts.model.TradeHistoryVO;
import com.wts.summary.dto.PortfolioItemDto;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.jpa.entity.TradeHistory;
import com.wts.util.PortfolioCalculator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class StockTradeAggregator {
    public static List<String> stockTradeTypes(BrokerType brokerType) {
        if (brokerType == BrokerType.TOSS) {
            return List.of("외화증권배당금입금", "구매", "판매", "주식병합출고", "주식병합입고");
        }
        if (brokerType == BrokerType.KIWOOM) {
            return List.of("보통매매 장내매도", "보통매매 장내매수", "수익분배금입금");
        }
        throw new IllegalArgumentException("지원하지 않는 증권사입니다: " + brokerType);
    }

    public static List<PortfolioItemDto> summarizeBySymbol(
            Long userId,
            BrokerType brokerType,
            List<TradeHistory> stockTrades,
            BiFunction<String, String, String> tickerResolver
    ) {
        if (stockTrades == null || stockTrades.isEmpty()) {
            return List.of();
        }

        Map<String, List<TradeHistory>> grouped = stockTrades.stream()
                .collect(Collectors.groupingBy(TradeHistory::getSymbolName));

        List<PortfolioItemDto> results = new ArrayList<>();
        for (Map.Entry<String, List<TradeHistory>> entry : grouped.entrySet()) {
            String symbolName = entry.getKey();
            List<TradeHistoryVO> vos = entry.getValue().stream()
                    .map(TradeHistoryVO::from)
                    .sorted(Comparator.comparing(TradeHistoryVO::trHistId))
                    .toList();

            if (vos.isEmpty()) {
                continue;
            }

            Currency currency = vos.get(0).amount().currency();
            String isin = vos.get(0).isin();
            String ticker = tickerResolver.apply(isin, symbolName);

            PortfolioPositionVO position = PortfolioCalculator.calculate(vos, currency);
            results.add(toPortfolioItem(userId, brokerType, symbolName, isin, ticker, currency, position));
        }

        return results;
    }

    private static PortfolioItemDto toPortfolioItem(
            Long userId,
            BrokerType brokerType,
            String symbolName,
            String isin,
            String ticker,
            Currency currency,
            PortfolioPositionVO position
    ) {
        return PortfolioItemDto.builder()
                .userId(userId)
                .companyName(symbolName)
                .buyQty(position.buyQty().value())
                .sellQty(position.sellQty().value())
                .quantity(position.holdingQty().value())
                .currency(currency)
                .totalBuy(position.totalBuy().amount())
                .totalSell(position.totalSell().amount())
                .avgBuyPrice(position.avgBuyPrice().amount())
                .avgSellPrice(position.avgSellPrice().amount())
                .profit(position.profit().amount())
                .dividend(position.divined().amount())
                .isin(isin)
                .symbol(ticker)
                .holdingAmount(position.holdingAmount().amount())
                .holdingPrice(position.holdingPrice().amount())
                .brokerType(brokerType)
                .build();
    }
}
