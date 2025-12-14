package com.wts.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.wts.model.StockInfo;
import lombok.*;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponseDto {

    @JsonAnySetter
    private Map<String, StockInfo> stocks = new HashMap<>();

    public void setStock(String symbol, StockInfo stockInfo) {
        stocks.put(symbol, stockInfo);
    }

    public StockInfo getStock(String symbol) {
        return stocks.get(symbol);
    }
}