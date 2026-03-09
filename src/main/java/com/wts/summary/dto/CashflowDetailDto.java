package com.wts.summary.dto;

import com.wts.summary.enums.FlowType;
import com.wts.summary.enums.InOut;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowDetailDto {
    private InOut mainCategory;
    private FlowType flowType;
    private LocalDate itemDate;
    private String itemName;
    private BigDecimal itemAmountKrw;
    private BigDecimal itemAmountUsd;
    private BigDecimal fxRate;
}
