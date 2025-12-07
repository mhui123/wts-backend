package com.wts.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfo {

    @JsonProperty("symbol")
    private String symbol;

    @JsonProperty("price")
    private BigDecimal price;

    @JsonProperty("open")
    private BigDecimal open;

    @JsonProperty("high")
    private BigDecimal high;

    @JsonProperty("low")
    private BigDecimal low;

    @JsonProperty("volume")
    private Long volume;

    @JsonProperty("lastUpdated")
    private LocalDateTime lastUpdated;

    @JsonProperty("marketCap")
    private Long marketCap;

    @JsonProperty("currency")
    private String currency;
}