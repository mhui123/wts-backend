package com.wts.model;

import com.wts.entity.DashboardDetail;
import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDetailDto {

    private Long userId;
    private String symbolName;
    private String ticker;
    private String tradeType;
    private BigDecimal quantity = BigDecimal.ZERO;
    private BigDecimal avgPriceKrw = BigDecimal.ZERO;
    private BigDecimal avgPriceUsd = BigDecimal.ZERO;
    private BigDecimal totalAmountKrw = BigDecimal.ZERO;
    private BigDecimal totalAmountUsd = BigDecimal.ZERO;
    private BigDecimal profitLossKrw = BigDecimal.ZERO;
    private BigDecimal profitLossUsd = BigDecimal.ZERO;
    private BigDecimal dividendKrw = BigDecimal.ZERO;
    private BigDecimal dividendUsd = BigDecimal.ZERO;
    private BigDecimal currentPriceKrw = BigDecimal.ZERO;
    private BigDecimal currentPriceUsd = BigDecimal.ZERO;
}

