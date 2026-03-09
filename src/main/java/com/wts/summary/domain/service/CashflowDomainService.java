package com.wts.summary.domain.service;

import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.enums.FlowType;
import com.wts.summary.enums.InOut;
import com.wts.summary.enums.KiwoomTypes;
import com.wts.summary.enums.TossTypes;
import com.wts.summary.enums.YesNo;
import com.wts.summary.jpa.entity.CashflowDetailEntity;
import com.wts.summary.jpa.entity.CashflowEntity;
import com.wts.summary.jpa.entity.TradeHistory;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@Slf4j
public class CashflowDomainService {

    public CashflowDetailEntity createDetail(CashflowEntity cashflowEntity, TradeHistory trade, Currency currency, BrokerType brokerType) {
        InOut inOut = brokerType.resolveInOut(trade.getTradeType(), currency);
        FlowType flowType = brokerType.resolve(trade.getTradeType(), currency);
        flowType = checkIsPayForTax(trade, flowType);

        String itemName = adjustItemName(trade, flowType);
        BigDecimal itemAmountKrw = resolveItemAmountKrw(trade, inOut, currency, flowType);
        BigDecimal itemAmountUsd = resolveItemAmountUsd(trade, inOut, currency, flowType);

        return CashflowDetailEntity.builder()
                .cashflowEntity(cashflowEntity)
                .mainCategory(inOut)
                .flowType(flowType)
                .itemDate(trade.getTradeDate())
                .itemName(itemName)
                .itemAmountKrw(itemAmountKrw)
                .itemAmountUsd(itemAmountUsd)
                .balanceKrw(trade.getBalanceKrw())
                .balanceUsd(trade.getBalanceUsd())
                .fxRate(trade.getFxRate())
                .build();
    }

    private BigDecimal resolveItemAmountKrw(TradeHistory trade, InOut inOut, Currency currency, FlowType flowType) {
        if (currency != Currency.KRW) {
            return trade.getAmountKrw();
        }

        BigDecimal amount = trade.getAmountKrw();
        BigDecimal fee = trade.getFeeKrw() == null ? BigDecimal.ZERO : trade.getFeeKrw();
        BigDecimal tax = trade.getTaxKrw() == null ? BigDecimal.ZERO : trade.getTaxKrw();

        if (InOut.IN.equals(inOut)) {
            if (flowType == FlowType.TAX_PAID) {
                return amount;
            }
            return amount == null ? null : amount.subtract(fee).subtract(tax);
        }
        if(flowType == FlowType.TAX_ACCRUED) {
            BigDecimal repay = trade.getRepayTotalKrw();
            if(repay.compareTo(BigDecimal.ZERO) == 0) {
                return amount;
            }
            return amount.subtract(repay); // 실제 납부된 세금은 amount - repay
        }
        return amount == null ? null : amount.add(fee).add(tax);
    }

    private BigDecimal resolveItemAmountUsd(TradeHistory trade, InOut inOut, Currency currency, FlowType flowType) {
        if (currency != Currency.USD) {
            return trade.getAmountUsd().setScale(2, RoundingMode.DOWN);
        }

        BigDecimal amount = trade.getAmountUsd();
        BigDecimal fee = trade.getFeeUsd() == null ? BigDecimal.ZERO : trade.getFeeUsd();
        BigDecimal tax = trade.getTaxUsd() == null ? BigDecimal.ZERO : trade.getTaxUsd();

        //외화증권배당금은 입금당시부터 세금이 차감된 상태이므로 비용처리에서 제외해야 한다.
        if (InOut.IN.equals(inOut)) {
            if (FlowType.DIVIDEND.equals(flowType)) {
                return amount;
            }
            return amount == null ? null : amount.subtract(fee).subtract(tax).setScale(2, RoundingMode.DOWN);
        }
        if (FlowType.DIVIDEND_CANCEL.equals(flowType)) {
            return amount;
        }
        return amount == null ? null : amount.add(fee).add(tax).setScale(2, RoundingMode.DOWN);
    }

    private String adjustItemName(TradeHistory trade, FlowType flowType) {
        if(flowType == FlowType.TAX_PAID) {
            return "세금납부";
        }
        return Strings.isBlank(trade.getSymbolName()) ? trade.getTradeType() : trade.getSymbolName();
    }

    private FlowType checkIsPayForTax(TradeHistory trade, FlowType flowType) {
        if (flowType != FlowType.DEPOSIT) {
            return flowType;
        }

        BigDecimal repay = trade.getTradeCurrency() == Currency.USD ? trade.getRepayTotalUsd() : trade.getRepayTotalKrw();
        if (repay.compareTo(BigDecimal.ZERO) == 0) {
            return flowType;
        }
        return FlowType.TAX_PAID;
    }

    public void updateMonthlySummary(CashflowEntity cashflowEntity,
                                     BigDecimal previousEndAmount,
                                     List<CashflowDetailEntity> details,
                                     Currency currency,
                                     LocalDate today) {
        cashflowEntity.setStartAmount(previousEndAmount);
        cashflowEntity.setInflowAmountKrw(BigDecimal.ZERO);
        cashflowEntity.setInflowAmountUsd(BigDecimal.ZERO);
        cashflowEntity.setOutflowAmountKrw(BigDecimal.ZERO);
        cashflowEntity.setOutflowAmountUsd(BigDecimal.ZERO);

        CashflowDetailEntity lastDetail = details.get(details.size() - 1);
        for (CashflowDetailEntity detail : details) {
            if (detail.getMainCategory() == InOut.IN) {
                if (detail.getFlowType() != FlowType.TAX_PAID) {
                    cashflowEntity.addInflow(Currency.KRW, detail.getItemAmountKrw());
                    cashflowEntity.addInflow(Currency.USD, detail.getItemAmountUsd());
                }
            } else if (detail.getMainCategory() == InOut.OUT) {
                cashflowEntity.addOutflow(Currency.KRW, detail.getItemAmountKrw());
                cashflowEntity.addOutflow(Currency.USD, detail.getItemAmountUsd());
            }

//            log.info("[{}][{}] {} IN {} OUT {} $ IN {} $ OUT {}", detail.getItemDate(), currency, detail.getItemName(),
//                    cashflowEntity.getInflowAmountKrw(), cashflowEntity.getOutflowAmountKrw(),
//                    cashflowEntity.getInflowAmountUsd(), cashflowEntity.getOutflowAmountUsd());
        }

        BigDecimal inSumK = cashflowEntity.getInflowAmountKrw();
        BigDecimal outSumK = cashflowEntity.getOutflowAmountKrw();
        BigDecimal inSumU = cashflowEntity.getInflowAmountUsd();
        BigDecimal outSumU = cashflowEntity.getOutflowAmountUsd();
        cashflowEntity.setNetCashflowKrw(inSumK.subtract(outSumK));
        cashflowEntity.setNetCashflowUsd(inSumU.subtract(outSumU));

        BigDecimal endAmount = currency == Currency.USD ? lastDetail.getBalanceUsd() : lastDetail.getBalanceKrw();
        cashflowEntity.setEndAmount(endAmount);
        if (isMonthClosed(cashflowEntity.getBaseYm(), today)) {
            cashflowEntity.setCalculateFlag(YesNo.Y);
        }
    }

    public boolean isMonthClosed(LocalDate baseYm, LocalDate today) {
        LocalDate endOfMonth = baseYm.withDayOfMonth(baseYm.lengthOfMonth());
        return !today.isBefore(endOfMonth);
    }

    public List<String> getInoutflowTypes(BrokerType brokerType, Currency currency) {
        List<String> inflowTypes = switch (brokerType) {
            case TOSS -> TossTypes.getTossTypesByCurrencyAndInOut(currency, InOut.IN);
            case KIWOOM -> KiwoomTypes.getKiwoomTypesByCurrencyAndInOut(currency, InOut.IN);
        };
        List<String> outflowTypes = switch (brokerType) {
            case TOSS -> TossTypes.getTossTypesByCurrencyAndInOut(currency, InOut.OUT);
            case KIWOOM -> KiwoomTypes.getKiwoomTypesByCurrencyAndInOut(currency, InOut.OUT);
        };

        List<String> inoutflowTypes = new java.util.ArrayList<>(inflowTypes);
        inoutflowTypes.addAll(outflowTypes);
        return inoutflowTypes;
    }

    public void verifyData(Long userId,
                           Currency currency,
                           List<CashflowEntity> cashflowEntities,
                           List<TradeHistory> histories,
                           Set<String> stockRewardTypes,
                           Function<CashflowEntity, List<CashflowDetailEntity>> detailFetcher) {
        Map<LocalDate, TradeHistory> lastTradeByMonth = new HashMap<>();
        Set<LocalDate> rewardOnlyMonths = new HashSet<>();
        for (TradeHistory history : histories) {
            LocalDate baseYm = history.getTradeDate().withDayOfMonth(1);
            if (stockRewardTypes.contains(history.getTradeType())) {
                rewardOnlyMonths.add(baseYm);
                continue;
            }
            rewardOnlyMonths.remove(baseYm);
            TradeHistory current = lastTradeByMonth.get(baseYm);
            if (current == null
                    || history.getTradeDate().isAfter(current.getTradeDate())
                    || (history.getTradeDate().isEqual(current.getTradeDate())
                    && history.getTrHistId() != null
                    && current.getTrHistId() != null
                    && history.getTrHistId() > current.getTrHistId())) {
                lastTradeByMonth.put(baseYm, history);
            }
        }

        BigDecimal startAmount = BigDecimal.ZERO;
        for (CashflowEntity cashflowEntity : cashflowEntities) {
            BigDecimal inflow = BigDecimal.ZERO;
            BigDecimal outflow = BigDecimal.ZERO;
            BigDecimal runningAmount = startAmount;

            List<CashflowDetailEntity> details = detailFetcher.apply(cashflowEntity);
            for (CashflowDetailEntity detail : details) {
                BigDecimal amount = currency == Currency.USD
                        ? nullToZero(detail.getItemAmountUsd())
                        : nullToZero(detail.getItemAmountKrw());
                BigDecimal before = runningAmount;
                BigDecimal expected = currency == Currency.USD ? detail.getBalanceUsd() : detail.getBalanceKrw();
                if (detail.getMainCategory() == InOut.IN) {
                    if (detail.getFlowType() == FlowType.TAX_PAID) {
                        log.info("[{}][{}] TAX_PAID {} {} -> {} (excluded from inflow), expected : {}",
                                detail.getItemDate(), currency, detail.getItemName(), before, runningAmount, expected);
                        if (expected.compareTo(BigDecimal.ZERO) == 0) {
                            continue;
                        }
                    }
                    inflow = inflow.add(amount);
                    runningAmount = runningAmount.add(amount);
                    log.info("[{}][{}] IN {} {} -> {} (+{}), expected : {}",
                            detail.getItemDate(), currency, detail.getItemName(), before, runningAmount, amount, expected);
                } else if (detail.getMainCategory() == InOut.OUT) {
                    if (detail.getFlowType() == FlowType.TAX_ACCRUED) {
                        log.info("[{}][{}] TAX_ACCRUED {} {} -> {} (excluded from outflow), expected : {}",
                                detail.getItemDate(), currency, detail.getItemName(), before, runningAmount, expected);
                        continue;
                    }
                    outflow = outflow.add(amount);
                    runningAmount = runningAmount.subtract(amount);
                    log.info("[{}][{}] OUT {} {} -> {} (-{}), expected : {}",
                            detail.getItemDate(), currency, detail.getItemName(), before, runningAmount, amount, expected);
                }
            }

            BigDecimal endAmount = runningAmount.setScale(2, RoundingMode.DOWN);
            TradeHistory lastTrade = lastTradeByMonth.get(cashflowEntity.getBaseYm());
            if (lastTrade == null) {
                if (rewardOnlyMonths.contains(cashflowEntity.getBaseYm())) {
                    startAmount = endAmount;
                    continue;
                }
                throw new IllegalStateException("월말 잔액 데이터가 없습니다. baseYm=" + cashflowEntity.getBaseYm()
                        + ", userId=" + userId + ", currency=" + currency);
            }

            BigDecimal expectedBalance = currency == Currency.USD
                    ? nullToZero(lastTrade.getBalanceUsd())
                    : nullToZero(lastTrade.getBalanceKrw());

            if (endAmount.compareTo(expectedBalance) != 0) {
                String message = "월말 잔액 불일치: userId=" + userId
                        + ", currency=" + currency
                        + ", baseYm=" + cashflowEntity.getBaseYm()
                        + ", start=" + startAmount
                        + ", inflow=" + inflow
                        + ", outflow=" + outflow
                        + ", end=" + endAmount
                        + ", expected=" + expectedBalance
                        + ", lastTradeId=" + lastTrade.getTrHistId()
                        + ", lastTradeDate=" + lastTrade.getTradeDate()
                        + ", lastTradeType=" + lastTrade.getTradeType()
                        + ", lastSymbol=" + lastTrade.getSymbolName();
                log.warn(message);
                throw new IllegalStateException(message);
            } else {
                log.info("월말 잔액 일치: [{}][{}] expected={}, endAmount={}",
                        cashflowEntity.getBaseYm(), currency, expectedBalance, endAmount);
            }

            startAmount = endAmount;
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
