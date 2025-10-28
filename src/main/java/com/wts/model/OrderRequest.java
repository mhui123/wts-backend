// 주문 요청 DTO: 클라이언트가 전송하는 주문 요청의 유효성 검사 규칙과 필드를 정의합니다.
// 주요 책임: symbol, side, qty, price 필드 보유 및 Jakarta Validation 어노테이션 적용
package com.wts.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class OrderRequest {
    @NotBlank
    private String symbol;

    @NotBlank
    private String side; // BUY or SELL

    @Min(1)
    private long qty;

    private Double price; // null for market

    public OrderRequest() {}

    public OrderRequest(String symbol, String side, long qty, Double price) {
        this.symbol = symbol;
        this.side = side;
        this.qty = qty;
        this.price = price;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public long getQty() { return qty; }
    public void setQty(long qty) { this.qty = qty; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
