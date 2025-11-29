package com.wts.model;

import java.math.BigDecimal;

public class SummaryRecord{
    private BigDecimal sumKrw;
    private BigDecimal sumUsd;

    public SummaryRecord(BigDecimal sumKrw, BigDecimal sumUsd) {
        this.sumKrw = sumKrw;
        this.sumUsd = sumUsd;
    }

    public BigDecimal sumKrw() {
        return sumKrw;
    }

    public BigDecimal sumUsd() {
        return sumUsd;
    }
}
