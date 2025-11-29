package com.wts.model;

import java.math.BigDecimal;

public interface SymbolAggregation {
    String getSymbolName();
    BigDecimal getQuantity();
    BigDecimal getSumK();
    BigDecimal getSumU();
}
