package com.wts.summary.jpa.repository.query;

import com.wts.model.TradeSearchCondition;
import com.wts.summary.jpa.entity.TradeHistory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TradeHistorySpecification {
    public static Specification<TradeHistory> withCondition(TradeSearchCondition condition) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (condition.getUserId() != null && condition.getUserId() > 0) {
                predicates.add(cb.equal(root.get("user").get("id"), condition.getUserId()));
            }

            if (condition.getTradeTypes() != null && !condition.getTradeTypes().isEmpty()) {
                predicates.add(root.get("tradeType").in(condition.getTradeTypes()));
            }

            if (condition.getSymbolName() != null) {
                predicates.add(cb.equal(root.get("symbolName"), condition.getSymbolName()));
            }

            if (condition.getStartDate() != null && condition.getEndDate() != null) {
                predicates.add(cb.between(root.get("tradeDate"),
                        condition.getStartDate(),
                        condition.getEndDate()));
            }

            if (condition.getCurrency() != null) {
                predicates.add(cb.equal(root.get("tradeCurrency"), condition.getCurrency()));
            }

            if (condition.getBrokerType() != null) {
                predicates.add(cb.equal(root.get("brokerType"), condition.getBrokerType()));
            }

            if (condition.getTradeDate() != null) {
                predicates.add(cb.equal(root.get("tradeDate"), condition.getTradeDate()));
            }

            if (condition.getTradeType() != null) {
                predicates.add(cb.equal(root.get("tradeType"), condition.getTradeType()));
            }

            if (condition.getQuantity() != null) {
                predicates.add(cb.equal(root.get("quantity"), condition.getQuantity()));
            }

            if (condition.getIsin() != null) {
                predicates.add(cb.equal(root.get("isin"), condition.getIsin()));
            }

            if (condition.getAmountKrw() != null) {
                predicates.add(cb.equal(root.get("amountKrw"), condition.getAmountKrw()));
            }

            if (condition.getBalanceKrw() != null) {
                predicates.add(cb.equal(root.get("balanceKrw"), condition.getBalanceKrw()));
            }

            if (condition.getOrderBy() != null
                    && !Long.class.equals(Objects.requireNonNull(query).getResultType())
                    && !long.class.equals(query.getResultType())) {
                var orderPath = switch (condition.getOrderBy()) {
                    case TR_HIST_ID -> root.get("trHistId");
                    case TRADE_DATE -> root.get("tradeDate");
                    case TRADE_TYPE -> root.get("tradeType");
                    case SYMBOL_NAME -> root.get("symbolName");
                    case AMOUNT_KRW -> root.get("amountKrw");
                    case AMOUNT_USD -> root.get("amountUsd");
                    case BROKER_TYPE -> root.get("brokerType");
                };
                var direction = condition.getSortDirection() == null
                        ? TradeSearchCondition.SortDirection.ASC
                        : condition.getSortDirection();
                query.orderBy(direction == TradeSearchCondition.SortDirection.DESC
                        ? cb.desc(orderPath)
                        : cb.asc(orderPath));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
