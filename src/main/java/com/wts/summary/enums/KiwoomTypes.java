package com.wts.summary.enums;

import lombok.Getter;

import java.util.List;
import java.util.Map;

import static com.wts.summary.enums.InOut.IN;
import static com.wts.summary.enums.InOut.OUT;


@Getter
public enum KiwoomTypes {
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

    KiwoomTypes(InOut inOut, FlowType flowType) {
        this.inOut = inOut;
        this.flowType = flowType;
    }

    private static final Map<String, KiwoomTypes> KRW_TOSS_MAP = Map.ofEntries(
            Map.entry("보통매매 장내매도", SELL),
            Map.entry("보통매매 장내매수", BUY),
            Map.entry("이체입금(지급결제)", DEPOSIT),
            Map.entry("수익분배금입금", DIVIDEND),
            Map.entry("배당세출금", TAX_ACCRUED),
            Map.entry("이자입금", INTEREST)
    );

    private static final Map<String, KiwoomTypes> USD_TOSS_MAP = Map.ofEntries(

    );

    private static Map<String, KiwoomTypes> choiceMap(Currency currency){
        if(currency == Currency.USD) {
            return USD_TOSS_MAP;
        } else {
            return KRW_TOSS_MAP;
        }

    }

    public static List<String> getKiwoomTypesByCurrencyAndInOut(Currency currency, InOut inOut) {
        return choiceMap(currency).entrySet().stream()
                .filter(entry -> entry.getValue().getInOut() == inOut)
                .map(Map.Entry::getKey)
                .toList();
    }

    public static KiwoomTypes from(String tradeType, Currency currency) {
        Map<String, KiwoomTypes> map = choiceMap(currency);

        KiwoomTypes type = map.get(tradeType);

        if (type == null) {
            throw new IllegalArgumentException(
                    "Unknown Kiwoom tradeType: " + tradeType + ", currency: " + currency
            );
        }

        return type;
    }

    public static KiwoomTypes tryFrom(String tradeType, Currency currency) {
        return choiceMap(currency).get(tradeType);
    }
}
