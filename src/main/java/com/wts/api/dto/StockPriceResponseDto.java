package com.wts.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceResponseDto {

    @JsonAnySetter
    private Map<String, StockInfo> stocks;

}