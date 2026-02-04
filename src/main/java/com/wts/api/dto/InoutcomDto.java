package com.wts.api.dto;

import com.wts.api.entity.PortfolioItem;
import com.wts.api.entity.TradeHistory;
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

    public void addToIncomeSum(BigDecimal amountKrw, BigDecimal amountUsd) {
        if(this.incomeSumKrw == null) {this.incomeSumKrw = BigDecimal.ZERO;}
        if(this.incomeSumUsd == null) {this.incomeSumUsd = BigDecimal.ZERO;}

        if (amountKrw != null) {
            this.incomeSumKrw = this.incomeSumKrw.add(amountKrw);
        }
        if (amountUsd != null) {
            this.incomeSumUsd = this.incomeSumUsd.add(amountUsd);
        }
    }

    public void addToOutcomeSum(BigDecimal amountKrw, BigDecimal amountUsd) {
        if(this.outcomeSumKrw == null) {this.outcomeSumKrw = BigDecimal.ZERO;}
        if(this.outcomeSumUsd == null) {this.outcomeSumUsd = BigDecimal.ZERO;}
        if (amountKrw != null) {
            this.outcomeSumKrw = this.outcomeSumKrw.add(amountKrw);
        }
        if (amountUsd != null) {
            this.outcomeSumUsd = this.outcomeSumUsd.add(amountUsd);
        }
    }

    public void addToDivSum(BigDecimal amountKrw, BigDecimal amountUsd) {
        if(this.divSumKrw == null) {this.divSumKrw = BigDecimal.ZERO;}
        if(this.divSumUsd == null) {this.divSumUsd = BigDecimal.ZERO;}

        if (amountKrw != null) {
            this.divSumKrw = this.divSumKrw.add(amountKrw);
        }
        if (amountUsd != null) {
            this.divSumUsd = this.divSumUsd.add(amountUsd);
        }
    }
}
