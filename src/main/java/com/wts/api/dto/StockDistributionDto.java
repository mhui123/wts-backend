package com.wts.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDistributionDto {
    private String ticker;
    private BigDecimal distributionPerShare;
    private BigDecimal rocPct;
    private LocalDate declaredDate;
    private LocalDate exDate;
    private LocalDate recordDate;
    private LocalDate payableDate;

    public StockDistributionDto(String ticker) {
        this.ticker = ticker;
    }
}

