package com.wts.service;

import com.wts.entity.TradeHistory;
import com.wts.model.TradeHistoryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TradeHistoryService {

    @PersistenceContext
    private EntityManager em;

    public List<TradeHistoryDto> getTrades(Long userId,
                                           LocalDate fromDate,
                                           LocalDate toDate,
                                           String tradeType,
                                           String symbolName,
                                           Integer page,
                                           Integer size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<TradeHistory> cq = cb.createQuery(TradeHistory.class);
        Root<TradeHistory> root = cq.from(TradeHistory.class);
        List<Predicate> predicates = new ArrayList<>();

        if (userId != null) {
            predicates.add(cb.equal(root.get("user").get("id"), userId));
        }
        if (fromDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("tradeDate"), fromDate));
        }
        if (toDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("tradeDate"), toDate));
        }
        if (tradeType != null && !tradeType.isBlank()) {
            predicates.add(cb.equal(root.get("tradeType"), tradeType));
        }
        if (symbolName != null && !symbolName.isBlank()) {
            predicates.add(cb.equal(root.get("symbolName"), symbolName));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("tradeDate")), cb.desc(root.get("trHistId")));

        var query = em.createQuery(cq);
        if (page != null && size != null) {
            int p = Math.max(0, page);
            int s = Math.max(1, size);
            query.setFirstResult(p * s);
            query.setMaxResults(s);
        }

        List<TradeHistory> results = query.getResultList();

        List<TradeHistoryDto> dtos = new ArrayList<>(results.size());
        for (TradeHistory t : results) {
            dtos.add(TradeHistoryDto.builder()
                    .trHistId(t.getTrHistId())
                    .userId(t.getUser() != null ? t.getUser().getId() : null)
                    .tradeDate(t.getTradeDate())
                    .tradeType(t.getTradeType())
                    .symbolName(t.getSymbolName())
                    .fxRate(t.getFxRate())
                    .quantity(t.getQuantity())
                    .amountKrw(t.getAmountKrw())
                    .amountUsd(t.getAmountUsd())
                    .priceKrw(t.getPriceKrw())
                    .priceUsd(t.getPriceUsd())
                    .feeKrw(t.getFeeKrw())
                    .feeUsd(t.getFeeUsd())
                    .taxKrw(t.getTaxKrw())
                    .taxUsd(t.getTaxUsd())
                    .repayTotalKrw(t.getRepayTotalKrw())
                    .repayTotalUsd(t.getRepayTotalUsd())
                    .balanceQty(t.getBalanceQty())
                    .balanceKrw(t.getBalanceKrw())
                    .balanceUsd(t.getBalanceUsd())
                    .sourceRow(t.getSourceRow())
                    .createdAt(t.getCreatedAt())
                    .updatedAt(t.getUpdatedAt())
                    .build());
        }

        return dtos;
    }
}

