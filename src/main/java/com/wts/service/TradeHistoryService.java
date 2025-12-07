package com.wts.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wts.api.OrdersController;
import com.wts.entity.TradeHistory;
import com.wts.entity.User;
import com.wts.model.*;
import com.wts.repository.TradeHistoryRepository;
import com.wts.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TradeHistoryService {
    private static final Logger log = LoggerFactory.getLogger(TradeHistoryService.class);

    @PersistenceContext
    private EntityManager em;

    private final TradeHistoryRepository repository;
    private final UserRepository uRepo;

    public TradeHistoryService(TradeHistoryRepository repository, UserRepository uRepo) {
        this.repository = repository;
        this.uRepo = uRepo;
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

    /**
     * Python 서버에서 받은 JSON 데이터를 TradeHistory로 변환하여 저장
     */
    @Transactional
    public ProcessResult saveTradeHistoryFromJson(String jsonData, Long userId) {
        try {
            // JSON 문자열을 List<TradeHistoryJsonDto>로 변환
            ObjectMapper objectMapper = new ObjectMapper();
            List<TradeHistoryJsonDto> jsonDtos = objectMapper.readValue(jsonData,
                    new TypeReference<List<TradeHistoryJsonDto>>() {});

            // User 엔티티 조회
            User user = uRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            List<TradeHistory> tradeHistories = new ArrayList<>();
            int processedCount = 0;

            for (TradeHistoryJsonDto dto : jsonDtos) {
                try {
                    TradeHistory tradeHistory = convertToTradeHistory(dto, user);
                    tradeHistories.add(tradeHistory);
                    processedCount++;
                } catch (Exception e) {
                    log.warn("거래내역 변환 실패: {}", e.getMessage());
                    // 개별 레코드 실패는 로그만 남기고 계속 진행
                }
            }

            // 일괄 저장
            List<TradeHistory> savedHistories = repository.saveAll(tradeHistories);

            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("총 %d건 중 %d건 저장 완료", jsonDtos.size(), savedHistories.size()))
                    .processedCount(savedHistories.size())
                    .totalCount(jsonDtos.size())
                    .build();

        } catch (Exception e) {
            log.error("JSON 데이터 저장 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("데이터 저장 실패: " + e.getMessage())
                    .processedCount(0)
                    .totalCount(0)
                    .build();
        }
    }

    /**
     * TradeHistoryJsonDto를 TradeHistory 엔티티로 변환
     */
    private TradeHistory convertToTradeHistory(TradeHistoryJsonDto dto, User user) {
        return TradeHistory.builder()
                .user(user)
                .tradeDate(convertTimestampToLocalDate(dto.getTradeDate()))
                .tradeType(dto.getTradeType())
                .symbolName(dto.getSymbolName())
                .fxRate(dto.getFxRate())
                .quantity(dto.getQuantity())
                .amountKrw(dto.getAmountKrw())
                .amountUsd(dto.getAmountUsd())
                .priceKrw(dto.getPriceKrw())
                .priceUsd(dto.getPriceUsd())
                .feeKrw(dto.getFeeKrw())
                .feeUsd(dto.getFeeUsd())
                .taxKrw(dto.getTaxKrw())
                .taxUsd(dto.getTaxUsd())
                .repayTotalKrw(dto.getRepayTotalKrw())
                .repayTotalUsd(dto.getRepayTotalUsd())
                .balanceQty(dto.getBalanceQty())
                .balanceKrw(dto.getBalanceKrw())
                .balanceUsd(dto.getBalanceUsd())
                .isin(dto.getIsin())
                .build();
    }

    /**
     * timestamp(밀리초)를 LocalDate로 변환
     */
    private LocalDate convertTimestampToLocalDate(Long timestamp) {
        if (timestamp == null) {
            return null;
        }
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }
}
