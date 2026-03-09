package com.wts.summary.domain.service;

import com.wts.auth.jpa.entity.User;
import com.wts.model.TradeSearchCondition;
import com.wts.summary.dto.TradeHistoryJsonRecord;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.jpa.entity.TradeHistory;
import com.wts.summary.jpa.repository.TradeHistoryRepository;
import com.wts.summary.jpa.repository.query.TradeHistorySpecification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TradeHistoryDomainService {
    private static final Logger log = LoggerFactory.getLogger(TradeHistoryDomainService.class);
    private static final int BATCH_SIZE = 500;

    private final TradeHistoryRepository repository;

    public SaveResult saveTrades(List<TradeHistoryJsonRecord> dtoList,
                                 Long userId,
                                 BrokerType brokerType,
                                 User user,
                                 int totalCountHint) {
        SaveAccumulator accumulator = new SaveAccumulator(userId, brokerType, user);
        for (TradeHistoryJsonRecord dto : dtoList) {
            accumulator.add(dto);
        }
        return accumulator.finish(totalCountHint);
    }

    public record SaveResult(int totalCount, int savedCount, int parsedCount) {
    }

    private final class SaveAccumulator {
        private final Long userId;
        private final BrokerType brokerType;
        private final User user;
        private final List<TradeHistory> batch = new ArrayList<>();
        private int savedCount = 0;
        private int parsedCount = 0;

        private SaveAccumulator(Long userId, BrokerType brokerType, User user) {
            this.userId = userId;
            this.brokerType = brokerType;
            this.user = user;
        }

        private void add(TradeHistoryJsonRecord dto) {
            if (dto == null) {
                return;
            }
            parsedCount++;
            if (isDuplicateTrade(userId, dto)) {
                return;
            }
            batch.add(convertToTradeHistory(dto, user, brokerType));
            if (batch.size() >= BATCH_SIZE) {
                flush();
            }
        }

        private SaveResult finish(int totalCountHint) {
            flush();
            int resolvedTotal = totalCountHint > 0 ? totalCountHint : parsedCount;
            return new SaveResult(resolvedTotal, savedCount, parsedCount);
        }

        private void flush() {
            if (batch.isEmpty()) {
                return;
            }
            savedCount += repository.saveAll(batch).size();
            batch.clear();
        }
    }

    private TradeHistory convertToTradeHistory(TradeHistoryJsonRecord dto, User user, BrokerType brokerType) {
        return TradeHistory.builder()
                .user(user)
                .tradeDate(convertStringToLocalDate(dto.tradeDate()))
                .tradeType(dto.tradeType())
                .symbolName(dto.symbolName())
                .fxRate(dto.fxRate())
                .quantity(dto.quantity())
                .amountKrw(dto.amountKrw())
                .amountUsd(dto.amountUsd())
                .priceKrw(dto.priceKrw())
                .priceUsd(dto.priceUsd())
                .feeKrw(dto.feeKrw())
                .feeUsd(dto.feeUsd())
                .taxKrw(dto.taxKrw())
                .taxUsd(dto.taxUsd())
                .repayTotalKrw(dto.repayTotalKrw())
                .repayTotalUsd(dto.repayTotalUsd())
                .balanceQty(dto.balanceQty())
                .balanceKrw(dto.balanceKrw())
                .balanceUsd(dto.balanceUsd())
                .isin(dto.isin())
                .brokerName(brokerType)
                .tradeCurrency(dto.tradeCurrency())
                .build();
    }

    private boolean isDuplicateTrade(Long userId, TradeHistoryJsonRecord dto) {
        TradeSearchCondition condition = TradeSearchCondition.builder()
                .userId(userId)
                .tradeDate(convertStringToLocalDate(dto.tradeDate()))
                .tradeType(dto.tradeType())
                .symbolName(dto.symbolName())
                .isin(dto.isin())
                .quantity(dto.quantity())
                .amountKrw(dto.amountKrw())
                .balanceKrw(dto.balanceKrw())
                .build();

        Optional<TradeHistory> existing = repository.findOne(TradeHistorySpecification.withCondition(condition));
        log.debug("{}", existing);
        return existing.isPresent();
    }

    private LocalDate convertStringToLocalDate(String dateStr) {
        if (dateStr == null) {
            return null;
        }
        return LocalDate.parse(dateStr);
    }
}

