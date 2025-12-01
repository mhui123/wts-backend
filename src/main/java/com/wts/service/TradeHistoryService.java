package com.wts.service;

import com.wts.entity.TradeHistory;
import com.wts.model.*;
import com.wts.repository.TradeHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TradeHistoryService {

    @PersistenceContext
    private EntityManager em;

    private final TradeHistoryRepository repository;

    public TradeHistoryService(TradeHistoryRepository repository) {
        this.repository = repository;
    }

    public Optional<DashboardStockDto> findLatestSymbolQuantity(Long userId, String symbolNamePattern, List<String> types) {
        Optional<TradeHistory> opt = repository.findFirstByUser_IdAndSymbolNameContainingAndTradeTypeInOrderByTradeDateDescTrHistIdDesc(
                userId, symbolNamePattern, types);
        return opt.map(t -> new DashboardStockDto(t.getSymbolName(), t.getBalanceQty()));
    }

    /**
     * Specification/QueryDSL 스타일로 리팩토링한 대체 메서드
     * 동적 조건을 Specification으로 조합하고 Spring Data Repository의 findAll(spec, pageable/sort)를 사용합니다.
     */
    public List<TradeHistoryDto> getTrades_renew(Long userId,
                                                 LocalDate fromDate,
                                                 LocalDate toDate,
                                                 String tradeType,
                                                 String symbolName,
                                                 Integer page,
                                                 Integer size) {
        Specification<TradeHistory> spec = Specification.where(null);

        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("user").get("id"), userId));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("tradeDate"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("tradeDate"), toDate));
        }
        if (tradeType != null && !tradeType.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tradeType"), tradeType));
        }
        if (symbolName != null && !symbolName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("symbolName"), symbolName));
        }

        Sort sort = Sort.by(Sort.Order.desc("tradeDate"), Sort.Order.desc("trHistId"));

        List<TradeHistory> entities;
        if (page != null && size != null) {
            int p = Math.max(0, page);
            int s = Math.max(1, size);
            Pageable pageable = PageRequest.of(p, s, sort);
            Page<TradeHistory> pg = repository.findAll(spec, pageable);
            entities = pg.getContent();
        } else {
            entities = repository.findAll(spec, sort);
        }

        return entities.stream()
                .map(t -> TradeHistoryDto.builder()
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
                        .build())
                .collect(Collectors.toList());
    }
}
