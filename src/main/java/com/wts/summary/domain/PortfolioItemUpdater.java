package com.wts.summary.domain;

import com.wts.summary.dto.PortfolioItemDto;
import com.wts.summary.jpa.entity.PortfolioItemEntity;

public class PortfolioItemUpdater {
    public static void applyUpdates(PortfolioItemEntity target, PortfolioItemDto source) {
        target.setProfit(source.getProfit());
        target.setQuantity(source.getQuantity());
        target.setTotalBuy(source.getTotalBuy());
        target.setTotalSell(source.getTotalSell());
        target.setBuyQty(source.getBuyQty());
        target.setSellQty(source.getSellQty());
        target.setAvgBuyPrice(source.getAvgBuyPrice());
        target.setAvgSellPrice(source.getAvgSellPrice());
        target.setDividend(source.getDividend());
        target.setFee(source.getFee());
        target.setTax(source.getTax());
        target.setSymbol(source.getSymbol());
        target.setHoldingAmount(source.getHoldingAmount());
        target.setHoldingPrice(source.getHoldingPrice());
        target.setBrokerType(source.getBrokerType());
    }

    public static PortfolioItemEntity toEntity(PortfolioItemDto source) {
        return PortfolioItemEntity.builder()
                .userId(source.getUserId())
                .companyName(source.getCompanyName())
                .symbol(source.getSymbol())
                .quantity(source.getQuantity())
                .currentPrice(source.getCurrentPrice())
                .totalSell(source.getTotalSell())
                .totalBuy(source.getTotalBuy())
                .buyQty(source.getBuyQty())
                .sellQty(source.getSellQty())
                .avgSellPrice(source.getAvgSellPrice())
                .avgBuyPrice(source.getAvgBuyPrice())
                .profit(source.getProfit())
                .dividend(source.getDividend())
                .tax(source.getTax())
                .fee(source.getFee())
                .currency(source.getCurrency())
                .holdingAmount(source.getHoldingAmount())
                .holdingPrice(source.getHoldingPrice())
                .brokerType(source.getBrokerType())
                .build();
    }
}
