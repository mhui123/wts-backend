package com.wts.summary.enums;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum BrokerType {

    TOSS {
        @Override
        public String uploadUri() {
            return "/wpy/uploadTradeHistory";
        }

        @Override
        public FlowType resolve(String tradeType, Currency currency) {
            return TossTypes.from(tradeType, currency).getFlowType();
        }

        @Override
        public InOut resolveInOut(String tradeType, Currency currency) {
            return TossTypes.from(tradeType, currency).getInOut();
        }

        @Override
        public List<String> typeToString(FlowType type, Currency currency) {
            return TossTypes.typeToStrings(type, currency);
        }
    },
    KIWOOM {
        @Override
        public String uploadUri() {
            return "/wpy/uploadKiwoomTradeHistory";
        }

        @Override
        public FlowType resolve(String tradeType, Currency currency) {
            return null;
        }

        @Override
        public InOut resolveInOut(String tradeType, Currency currency) {
            throw new UnsupportedOperationException("KIWOOM resolveInOut not implemented");
        }

        @Override
        public List<String> typeToString(FlowType type, Currency currency) {
            return new ArrayList<>();
        }
    };

    public abstract String uploadUri();
    public abstract FlowType resolve(String tradeType, Currency currency);
    public abstract InOut resolveInOut(String tradeType, Currency currency);
    public abstract List<String> typeToString(FlowType type, Currency currency);
}
