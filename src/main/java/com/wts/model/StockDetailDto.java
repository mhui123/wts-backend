package com.wts.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StockDetailDto {
    String ticker;
    List<DividendDetailDto> receivedInfo;
    List<StockDistributionDto> declaredInfo;

    public StockDetailDto(String ticker) {
        this.ticker = ticker;
    }
}
