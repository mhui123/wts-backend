package com.wts.kiwoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
public class StockDetailInfo {
    String stockCd;
    String stockNm;
    BigDecimal nowPrice;
    BigDecimal yesterdayClosePrice;
    BigDecimal changePrice; //전일대비 변동가격
    BigDecimal changeDirection; //0, 1, 2 : 상승
    String changeRate; //전일대비 변동률
    BigDecimal tradeVolume; //거래량
    BigDecimal tradeValue; //거래대금
    BigDecimal highPrice; //고가
    BigDecimal lowPrice; //저가
    BigDecimal openingPrice; //시가
    BigDecimal closePrice; //종가
    BigDecimal upLimitPrice; //상한가
    BigDecimal lowLimitPrice; //하한가
}
