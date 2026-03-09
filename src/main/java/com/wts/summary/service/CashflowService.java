package com.wts.summary.service;

import com.wts.summary.dto.CashflowDetailDto;
import com.wts.summary.dto.CashflowDto;
import com.wts.model.TradeSearchCondition;
import com.wts.summary.enums.*;
import com.wts.summary.enums.Currency;
import com.wts.summary.jpa.entity.CashflowEntity;
import com.wts.summary.jpa.entity.CashflowDetailEntity;
import com.wts.summary.jpa.entity.TradeHistory;
import com.wts.auth.jpa.entity.User;
import com.wts.api.dto.ProcessResult;
import com.wts.summary.jpa.repository.CashFlowDetailRepository;
import com.wts.summary.jpa.repository.CashFlowRepository;
import com.wts.summary.jpa.repository.TradeHistoryRepository;
import com.wts.summary.domain.service.CashflowDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CashflowService {

    private static final Set<String> STOCK_REWARD_TYPES = Set.of("주식퀴즈이벤트입고", "미션이벤트주식입고", "출석체크이벤트입고");

    private final CashFlowRepository cashFlowRepository;
    private final CashFlowDetailRepository cashFlowDetailRepository;
    private final TradeHistoryService tradeHistoryService;
    private final TradeHistoryRepository tradeHistoryRepository;
    private final CashflowDomainService cashflowDomainService;
    private BrokerType brokerType;
    private List<CashflowEntity> needCalculates;

    public ProcessResult getCashFlow(Long userId, LocalDate startYm, LocalDate endYm) {
        startYm = startYm.withDayOfMonth(1); //날짜를 1일로 보정.
        endYm = endYm.withDayOfMonth(1); //날짜를 1일로 보정.
        List<CashflowEntity> cashflowEntities = cashFlowRepository.findByUserIdAndBaseYmBetween(userId, startYm, endYm)
                .orElseThrow(() -> new RuntimeException("해당 월의 Cashflow가 존재하지 않습니다."));
        List<CashflowDto> listDtos = cashflowEntities.stream()
                .map(c -> CashflowDto.builder()
                        .userId(userId)
                        .baseYm(c.getBaseYm())
                        .currency(c.getCurrency())
                        .account(c.getAccount())
                        .startAmount(c.getStartAmount())
                        .inflowAmountKrw(c.getInflowAmountKrw())
                        .inflowAmountUsd(c.getInflowAmountUsd())
                        .outflowAmountKrw(c.getOutflowAmountKrw())
                        .outflowAmountUsd(c.getOutflowAmountUsd())
                        .netCashflowKrw(c.getNetCashflowKrw())
                        .netCashflowUsd(c.getNetCashflowUsd())
                        .endAmount(c.getEndAmount())
                        .build())
                .toList();

        return ProcessResult.builder().success(true).data(listDtos).build();
    }

    public ProcessResult getFlowDetails(Long userId, LocalDate baseYm, Currency currency) {
        baseYm = baseYm.withDayOfMonth(1); //날짜를 1일로 보정.
        CashflowEntity cashflowEntity = cashFlowRepository.findByUserIdAndBaseYmAndCurrency(userId, baseYm, currency)
                .orElseThrow(() -> new RuntimeException("해당 월의 Cashflow가 존재하지 않습니다."));

        List<CashflowDetailEntity> details = cashFlowDetailRepository.findByCashflowEntityOrderByItemDateAsc(cashflowEntity);
        List<CashflowDetailDto> detailDtos = details.stream()
                .map(d -> CashflowDetailDto.builder()
                        .mainCategory(d.getMainCategory())
                        .flowType(d.getFlowType())
                        .itemDate(d.getItemDate())
                        .itemName(d.getItemName())
                        .itemAmountKrw(d.getItemAmountKrw())
                        .itemAmountUsd(d.getItemAmountUsd())
                        .fxRate(d.getFxRate())
                        .build())
                .toList();

        return ProcessResult.builder().success(true).data(detailDtos).build();
    }

    public void calculateCashFlow(Long userId, String brokerType) {
        this.brokerType = BrokerType.valueOf(brokerType.toUpperCase());
        calculate(userId, Currency.KRW);
        calculate(userId, Currency.USD);
    }

    private void calculate(Long userId, Currency currency){
        List<String> inoutflowTypes = cashflowDomainService.getInoutflowTypes(brokerType, currency);

        TradeSearchCondition flowCondition = TradeSearchCondition.builder()
                .userId(userId)
                .tradeTypes(inoutflowTypes)
                .currency(currency)
                .orderBy(TradeSearchCondition.OrderBy.TR_HIST_ID)
                .sortDirection(TradeSearchCondition.SortDirection.ASC)
                .build();

        List<CashflowEntity> existingCashflowEntities = cashFlowRepository.findByUserIdAndCurrencyOrderByBaseYmAsc(userId, currency);
        needCalculates = cashFlowRepository.findByUserIdAndCurrencyAndCalculateFlag(userId, currency, YesNo.N)
                .orElse(Collections.emptyList());

        if (existingCashflowEntities.isEmpty()) {
            log.info("cashflow_master 초기 상태입니다. 전체 거래내역으로 계산합니다. userId={}, currency={}", userId, currency);
        } else {
            LocalDate targetYm = LocalDate.now().withDayOfMonth(1);

            Optional<LocalDate> lastCalculatedYmOpt = existingCashflowEntities.stream()
                    .filter(c -> c.getCalculateFlag() == YesNo.Y)
                    .map(CashflowEntity::getBaseYm)
                    .max(LocalDate::compareTo);

            LocalDate startYm;
            LocalDate endYm;

            if (lastCalculatedYmOpt.isPresent()) {
                LocalDate incrementalStartYm = lastCalculatedYmOpt.get().plusMonths(1);

                if (needCalculates.isEmpty()) {
                    // 핵심: needCalculates가 없어도 [마지막 완료월+1 .. targetYm] 구간은 계산
                    if (incrementalStartYm.isAfter(targetYm)) {
                        log.info("신규 계산 구간이 없습니다. userId={}, currency={}, lastCalculatedYm={}, targetYm={}",
                                userId, currency, lastCalculatedYmOpt.get(), targetYm);
                        return;
                    }
                    startYm = incrementalStartYm;
                    endYm = targetYm;
                } else {
                    LocalDate recalcStartYm = needCalculates.stream()
                            .map(CashflowEntity::getBaseYm)
                            .min(LocalDate::compareTo)
                            .orElseThrow();
                    LocalDate recalcEndYm = needCalculates.stream()
                            .map(CashflowEntity::getBaseYm)
                            .max(LocalDate::compareTo)
                            .orElseThrow();

                    // 재계산 구간 + 신규 증분 구간을 함께 포함
                    startYm = recalcStartYm.isBefore(incrementalStartYm) ? recalcStartYm : incrementalStartYm;
                    endYm = recalcEndYm.isAfter(targetYm) ? recalcEndYm : targetYm;
                }
            } else {
                // Y 이력이 없으면 needCalculates 기준으로 계산 (안전 fallback)
                if (needCalculates.isEmpty()) {
                    log.info("계산 대상이 없습니다. userId={}, currency={}", userId, currency);
                    return;
                }
                startYm = needCalculates.stream().map(CashflowEntity::getBaseYm).min(LocalDate::compareTo).orElseThrow();
                endYm = needCalculates.stream().map(CashflowEntity::getBaseYm).max(LocalDate::compareTo).orElseThrow();
            }

            flowCondition.setStartDate(startYm);
            flowCondition.setEndDate(endYm.withDayOfMonth(endYm.lengthOfMonth()));

            log.info("Cashflow 계산 구간 확정. userId={}, currency={}, startYm={}, endYm={}",
                    userId, currency, startYm, endYm);
        }

        List<TradeHistory> flows = tradeHistoryService.getHistoryWithinConditions(flowCondition);

        settingCashFlow(flows, currency);

        deployCashFlowDetail(flows, currency);
        summarizeDetailToMaster(currency);
    }

    @Transactional
    public void settingCashFlow(List<TradeHistory> flow, Currency currency){
        for(TradeHistory t : flow){
            User user = t.getUser();
            LocalDate baseDate = t.getTradeDate().withDayOfMonth(1);

            cashFlowRepository.findByUserAndBaseYmAndCurrencyAndCalculateFlag(user, baseDate, currency, YesNo.N)
                    .orElseGet(() -> {
                        CashflowEntity newCashflowEntity = CashflowEntity.builder()
                                .user(user)
                                .baseYm(baseDate)
                                .currency(currency)
                                .account(t.getBrokerName())
                                .calculateFlag(YesNo.N)
                                .startAmount(BigDecimal.ZERO)
                                .endAmount(BigDecimal.ZERO)
                                .build();
                        return cashFlowRepository.save(newCashflowEntity);
                    });
        }
    }

    public void deployCashFlowDetail(List<TradeHistory> flow, Currency currency){
        try{
            Set<String> clearedKeys = new HashSet<>();

            for (TradeHistory t : flow) {
//                log.info("flow 확인 : [{}][{}] {} {}", t.getTrHistId(), t.getTradeDate(), t.getTradeType(), t.getSymbolName());
                User user = t.getUser();
                LocalDate baseDate = t.getTradeDate().withDayOfMonth(1);
                CashflowEntity cashflowEntity = cashFlowRepository.findByUserAndBaseYmAndCurrency(user, baseDate, currency)
                        .orElseThrow(() -> new RuntimeException("해당 월의 Cashflow가 존재하지 않습니다."));

                // 같은 월/통화에 대해 기존 detail은 최초 1회만 삭제
                String clearKey = user.getId() + "|" + baseDate + "|" + currency;
                if (clearedKeys.add(clearKey)) {
                    List<CashflowDetailEntity> existingDetails = cashFlowDetailRepository.findByCashflowEntity(cashflowEntity);
                    if (!existingDetails.isEmpty()) {
                        cashFlowDetailRepository.deleteAllInBatch(existingDetails);
                    }

                    log.info("CashflowDetail 초기화 완료. clearedKey={} removed detail count : {}",clearKey, existingDetails.size());
                }

                CashflowDetailEntity detail = cashflowDomainService.createDetail(cashflowEntity, t, currency, brokerType);
                cashFlowDetailRepository.save(detail);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Transactional
    public void summarizeDetailToMaster(Currency currency){
        LocalDate today = LocalDate.now();
        BigDecimal previousEndAmount = BigDecimal.ZERO;
        for (CashflowEntity cashflowEntity : needCalculates) {
            List<CashflowDetailEntity> details = cashFlowDetailRepository.findByCashflowEntity(cashflowEntity);
            cashflowDomainService.updateMonthlySummary(cashflowEntity, previousEndAmount, details, currency, today);
            previousEndAmount = cashflowEntity.getEndAmount();
        }
    }

    public void verifyData(Long userId, Currency currency) {
        if (currency != Currency.USD && currency != Currency.KRW) {
            throw new IllegalArgumentException("지원하지 않는 통화입니다: " + currency);
        }

        List<CashflowEntity> cashflowEntities = cashFlowRepository.findByUserIdAndCurrencyOrderByBaseYmAsc(userId, currency);
        List<TradeHistory> histories = tradeHistoryRepository.findByUserIdOrderByTradeDateAsc(userId)
                .stream()
                .filter(history -> history.getTradeCurrency() == currency)
                .toList();

        cashflowDomainService.verifyData(
                userId,
                currency,
                cashflowEntities,
                histories,
                STOCK_REWARD_TYPES,
                cashFlowDetailRepository::findByCashflowEntity
        );
    }
}
