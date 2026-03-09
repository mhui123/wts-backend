package com.wts.model;

public record TradeType(String type) {
    public static TradeType from(String tradeType) {
        return new TradeType(tradeType);
    }

    public boolean isBuy() { return type.equals("구매"); }
    public boolean isSell() { return type.equals("판매"); }
    public boolean isStockIn() { return type.contains("입고"); }
    public boolean isStockOut() { return type.contains("출고"); }
    public boolean isDividend() { return type.endsWith("배당금입금"); }
    public boolean isDividendCancel() { return type.endsWith("배당금취소출금"); }
}
