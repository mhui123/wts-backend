package com.wts.model;

import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
public class TradeSearchCondition {
    private Long userId;
    private List<String> tradeTypes;
    private String symbolName;
    private LocalDate startDate; //yyyy-MM-dd 형태로
    private LocalDate endDate;
    private Currency currency;
    private OrderBy orderBy;
    private SortDirection sortDirection;
    private BrokerType brokerType;
    private LocalDate tradeDate;
    private String tradeType;
    private String isin;
    private BigDecimal quantity;
    private BigDecimal amountKrw;
    private BigDecimal balanceKrw;
    private Integer page;
    private Integer size;

    public enum OrderBy {
        TR_HIST_ID,
        TRADE_DATE,
        TRADE_TYPE,
        SYMBOL_NAME,
        AMOUNT_KRW,
        AMOUNT_USD,
        BROKER_TYPE
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}
