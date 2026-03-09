package com.wts.summary.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InoutcomDto {
    public List<PriceDto> moneyIns;
    public List<PriceDto> moneyOuts;
    public List<PriceDto> divIns;
    private BigDecimal incomeSumKrw;
    private BigDecimal outcomeSumKrw;
    private BigDecimal divSumKrw;
    private BigDecimal incomeSumUsd;
    private BigDecimal outcomeSumUsd;
    private BigDecimal divSumUsd;
    private BigDecimal otherSumKrw;
    private BigDecimal otherSumUsd;
}
