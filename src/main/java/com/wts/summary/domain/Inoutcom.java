package com.wts.summary.domain;


import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.enums.FlowType;
import com.wts.summary.enums.InOut;
import com.wts.summary.enums.KiwoomTypes;
import com.wts.summary.enums.TossTypes;
import com.wts.summary.jpa.entity.TradeHistory;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
public class Inoutcom {
    private BigDecimal incomeSumKrw = BigDecimal.ZERO;
    private BigDecimal outcomeSumKrw = BigDecimal.ZERO;
    private BigDecimal divSumKrw = BigDecimal.ZERO;
    private BigDecimal otherSumKrw = BigDecimal.ZERO;
    private BigDecimal incomeSumUsd = BigDecimal.ZERO;
    private BigDecimal outcomeSumUsd = BigDecimal.ZERO;
    private BigDecimal divSumUsd = BigDecimal.ZERO;
    private BigDecimal otherSumUsd = BigDecimal.ZERO;

    private enum SummaryBucket {
        INCOME,
        OUTCOME,
        DIVIDEND,
        OTHER,
        IGNORE
    }

    private record ResolvedType(FlowType flowType, InOut inOut) {
    }

    public static Inoutcom summarize(List<TradeHistory> histories, Currency currency) {
        Inoutcom result = new Inoutcom();
        if (histories == null || histories.isEmpty()) {
            return result;
        }

        for (TradeHistory history : histories) {
            Currency historyCurrency = history.getTradeCurrency() == null ? currency : history.getTradeCurrency();
            if (historyCurrency != currency) {
                continue;
            }

            ResolvedType resolved = resolveType(history, historyCurrency);
            if (resolved == null) {
                continue;
            }

            SummaryBucket bucket = resolveBucket(resolved.flowType());
            switch (bucket) {
                case INCOME -> result.addIncome(history.getAmountKrw(), history.getAmountUsd());
                case OUTCOME -> result.addOutcome(history.getAmountKrw(), history.getAmountUsd());
                case DIVIDEND -> result.addDividend(history.getAmountKrw(), history.getAmountUsd());
                case OTHER -> result.addOtherSigned(history.getAmountKrw(), history.getAmountUsd(), resolved.inOut());
                case IGNORE -> {
                    // Skip unsupported flow types for this summary.
                }
            }
        }

        return result;
    }

    private static ResolvedType resolveType(TradeHistory history, Currency currency) {
        BrokerType brokerType = history.getBrokerName();
        String tradeType = history.getTradeType();
        if (brokerType == null || tradeType == null) {
            return null;
        }

        return switch (brokerType) {
            case TOSS -> toResolved(TossTypes.tryFrom(tradeType, currency));
            case KIWOOM -> toResolved(KiwoomTypes.tryFrom(tradeType, currency));
        };
    }

    private static ResolvedType toResolved(TossTypes type) {
        if (type == null) {
            return null;
        }
        return new ResolvedType(type.getFlowType(), type.getInOut());
    }

    private static ResolvedType toResolved(KiwoomTypes type) {
        if (type == null) {
            return null;
        }
        return new ResolvedType(type.getFlowType(), type.getInOut());
    }

    private static SummaryBucket resolveBucket(FlowType flowType) {
        if (flowType == null) {
            return SummaryBucket.IGNORE;
        }

        return switch (flowType) {
            case DEPOSIT -> SummaryBucket.INCOME;
            case WITHDRAW -> SummaryBucket.OUTCOME;
            case DIVIDEND -> SummaryBucket.DIVIDEND;
            case INTEREST, TAX_ACCRUED, TAX_PAID, FX_GAIN, FX_LOSS, FEE, DIVIDEND_CANCEL -> SummaryBucket.OTHER;
            case BUY, SELL, STOCK_REWARD -> SummaryBucket.IGNORE;
        };
    }

    private void addOtherSigned(BigDecimal amountKrw, BigDecimal amountUsd, InOut inOut) {
        if (inOut == InOut.IN) {
            addOther(amountKrw, amountUsd);
            return;
        }
        addOther(negateOrNull(amountKrw), negateOrNull(amountUsd));
    }

    private void addIncome(BigDecimal amountKrw, BigDecimal amountUsd) {
        incomeSumKrw = add(incomeSumKrw, amountKrw);
        incomeSumUsd = add(incomeSumUsd, amountUsd);
    }

    private void addOutcome(BigDecimal amountKrw, BigDecimal amountUsd) {
        outcomeSumKrw = add(outcomeSumKrw, amountKrw);
        outcomeSumUsd = add(outcomeSumUsd, amountUsd);
    }

    private void addDividend(BigDecimal amountKrw, BigDecimal amountUsd) {
        divSumKrw = add(divSumKrw, amountKrw);
        divSumUsd = add(divSumUsd, amountUsd);
    }

    private void addOther(BigDecimal amountKrw, BigDecimal amountUsd) {
        otherSumKrw = add(otherSumKrw, amountKrw);
        otherSumUsd = add(otherSumUsd, amountUsd);
    }

    private static BigDecimal add(BigDecimal base, BigDecimal amount) {
        return amount == null ? base : base.add(amount);
    }

    private static BigDecimal negateOrNull(BigDecimal amount) {
        return amount == null ? null : amount.negate();
    }
}
