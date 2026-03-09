package com.wts.summary.jpa.mapper;

import com.wts.model.Money;
import com.wts.model.Quantity;
import com.wts.summary.domain.portfolio.Portfolio;
import com.wts.summary.domain.portfolio.PortfolioItem;
import com.wts.summary.jpa.entity.PortfolioItemEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PortfolioMapper {

    public Portfolio toDomain(List<PortfolioItemEntity> entities) {

        List<PortfolioItem> items = entities.stream()
                .map(this::toItem)
                .toList();

        return new Portfolio(items);
    }

    private PortfolioItem toItem(PortfolioItemEntity e) {
        return PortfolioItem.builder()
                .id(e.getId())
                .symbol(e.getSymbol())
                .quantity(Quantity.of(e.getQuantity()))
                .totalBuy(Money.of(e.getTotalBuy(), e.getCurrency()))
                .totalSell(Money.of(e.getTotalSell(), e.getCurrency()))
                .dividend(Money.of(e.getDividend(), e.getCurrency()))
                .profit(Money.of(e.getProfit(), e.getCurrency()))
                .currency(e.getCurrency())
                .build();
    }
}
