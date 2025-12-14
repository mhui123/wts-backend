package com.wts.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    // 총 투자금(USD 기준)
    private BigDecimal totalInvestmentKrw;
    private BigDecimal totalInvestmentUsd;
    private BigDecimal totalDividendKrw;
    private BigDecimal totalDividendUsd;
    private BigDecimal totalProfitKrw;
    private BigDecimal totalProfitUsd;
    private List<PortfolioItemDto> detailList;

    // 추후 필요시 KRW 환산값 등을 추가 가능

}

