package com.wts.api.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TickerResponseDto {

    private boolean success;                    // 성공 여부
    private String message;                     // 응답 메시지
    private List<TickerInfo> tickers;          // 티커 정보 리스트
    private LocalDateTime timestamp;            // 응답 시간
    private String errorCode;                   // 에러 코드 (실패시)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TickerInfo {
        private String symbol;                  // 티커 심볼
        private String name;                    // 종목명
        private String isin;                    // ISIN 코드
        private BigDecimal currentPrice;        // 현재가
        private BigDecimal changePercent;       // 변동률
        private String currency;                // 통화
        private String market;                  // 시장 (NASDAQ, NYSE 등)
        private LocalDateTime lastUpdate;       // 마지막 업데이트 시간
    }
}