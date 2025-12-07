package com.wts.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRecord {
    private Long trHistId;                  // 거래내역 ID
    private LocalDate tradeDate;            // 거래일자
    private String tradeType;               // 거래구분
    private String symbolName;              // 종목명
    private String isin;                    // ISIN 코드
    private BigDecimal fxRate;              // 환율
    private BigDecimal quantity;            // 수량
    private BigDecimal amountKrw;           // 거래대금(원)
    private BigDecimal amountUsd;           // 거래대금(달러)
    private BigDecimal priceKrw;            // 단가(원)
    private BigDecimal priceUsd;            // 단가(달러)
    private BigDecimal feeKrw;              // 수수료(원)
    private BigDecimal feeUsd;              // 수수료(달러)
    private BigDecimal taxKrw;              // 제세금(원)
    private BigDecimal taxUsd;              // 제세금(달러)
    private BigDecimal balanceQty;          // 잔고수량
    private BigDecimal balanceKrw;          // 잔액(원)
    private BigDecimal balanceUsd;          // 잔액(달러)
}