package com.wts.summary.dto;

import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowDto {

    private Long userId;
    private LocalDate baseYm;
    private BigDecimal startAmount;
    private BigDecimal endAmount;
    private Currency currency;
    private BrokerType account;
    private BigDecimal inflowAmountKrw;
    private BigDecimal outflowAmountKrw;
    private BigDecimal inflowAmountUsd;
    private BigDecimal outflowAmountUsd;
    private BigDecimal netCashflowKrw;
    private BigDecimal netCashflowUsd;

}
