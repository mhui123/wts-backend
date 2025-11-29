package com.wts.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    // 총 투자금(USD 기준)
    private BigDecimal totalInvestmentKrw;
    private BigDecimal totalInvestmentUsd;
    private List<Optional<DashboardStockDto>> stockList;

    // 추후 필요시 KRW 환산값 등을 추가 가능
}

