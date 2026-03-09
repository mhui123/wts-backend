package com.wts.summary.enums;

import lombok.Getter;

import java.util.List;
import java.util.Map;

import static com.wts.summary.enums.InOut.IN;
import static com.wts.summary.enums.InOut.OUT;


@Getter
public enum TossTypes {
    DEPOSIT(IN, FlowType.DEPOSIT),
    WITHDRAW(OUT, FlowType.WITHDRAW),
    BUY(OUT, FlowType.BUY),
    SELL(IN, FlowType.SELL),
    DIVIDEND(IN, FlowType.DIVIDEND),
    DIVIDEND_CANCEL(OUT, FlowType.DIVIDEND_CANCEL),
    FEE(OUT, FlowType.FEE),
    TAX_ACCRUED(OUT, FlowType.TAX_ACCRUED),
    TAX_PAID(OUT, FlowType.TAX_PAID),
    FX_GAIN(IN, FlowType.FX_GAIN),
    FX_LOSS(OUT, FlowType.FX_LOSS),
    STOCK_REWARD(IN, FlowType.STOCK_REWARD),
    INTEREST(IN, FlowType.INTEREST);

    private final InOut inOut;
    private final FlowType flowType;

    TossTypes(InOut inOut, FlowType flowType) {
        this.inOut = inOut;
        this.flowType = flowType;
    }

    private static final Map<String, TossTypes> KRW_TOSS_MAP = Map.ofEntries(
            Map.entry("판매", SELL),
            Map.entry("구매", BUY),
            Map.entry("오픈뱅킹입금", DEPOSIT),
            Map.entry("환전원화입금", DEPOSIT),
            Map.entry("환전원화출금", WITHDRAW),
            Map.entry("이체입금", DEPOSIT),
            Map.entry("이체입금(토스뱅크)", DEPOSIT),
            Map.entry("이체출금(토스뱅크)", WITHDRAW),
            Map.entry("외화이자세금출금", TAX_ACCRUED),
            Map.entry("배당세출금", TAX_ACCRUED),
            Map.entry("주식퀴즈이벤트입고", STOCK_REWARD),
            Map.entry("미션이벤트주식입고", STOCK_REWARD),
            Map.entry("출석체크이벤트입고", STOCK_REWARD),
            Map.entry("이자입금", INTEREST)
    );

    private static final Map<String, TossTypes> USD_TOSS_MAP = Map.ofEntries(
            Map.entry("판매", SELL),
            Map.entry("구매", BUY),
            Map.entry("환전외화입금", DEPOSIT),
            Map.entry("외화증권배당금입금", DIVIDEND),
            Map.entry("외화배당단주대금입금", DIVIDEND),
            Map.entry("외화세금환급", DEPOSIT),
            Map.entry("외화이자입금", DEPOSIT),
            Map.entry("외화배당금취소출금", DIVIDEND_CANCEL),
            Map.entry("외국납부세액출금", TAX_PAID),
            Map.entry("환전외화출금", WITHDRAW),
            Map.entry("환전외화입금취소", WITHDRAW),
            Map.entry("주식퀴즈이벤트입고", STOCK_REWARD),
            Map.entry("미션이벤트주식입고", STOCK_REWARD)
    );

    private static Map<String, TossTypes> choiceMap(Currency currency){
        if(currency == Currency.USD) {
            return USD_TOSS_MAP;
        } else {
            return KRW_TOSS_MAP;
        }

    }

    public static List<String> getTossTypesByCurrencyAndInOut(Currency currency, InOut inOut) {
        return choiceMap(currency).entrySet().stream()
                .filter(entry -> entry.getValue().getInOut() == inOut)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static TossTypes from(String tradeType, Currency currency) {
        Map<String, TossTypes> map = choiceMap(currency);

        TossTypes type = map.get(tradeType);

        if (type == null) {
            throw new IllegalArgumentException(
                    "Unknown Toss tradeType: " + tradeType + ", currency: " + currency
            );
        }

        return type;
    }

    public static TossTypes tryFrom(String tradeType, Currency currency) {
        return choiceMap(currency).get(tradeType);
    }

    public static List<String> typeToStrings(FlowType type, Currency currency) {
        return choiceMap(currency).entrySet().stream()
                .filter(entry -> entry.getValue().getFlowType() == type)
                .map(Map.Entry::getKey)
                .toList();
    }
}
