package com.wts.kiwoom.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StockDto {
    private String stockCd;
    private String stockNm;
    private String market;
}
