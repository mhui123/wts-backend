package com.wts.admin.mapper;

import com.wts.api.dto.StockDistributionDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class AdminAssembler {

    public static StockDistributionDto toDistributionDto(String ticker, Map<String, Object> fields) {
        return StockDistributionDto.builder()
                .ticker(ticker)
                .distributionPerShare(parseBigDecimal(fields.get("distribution_per_share")))
                .rocPct(parseBigDecimal(fields.get("roc_pct")))
                .declaredDate(parseLocalDate(fields.get("declared_date")))
                .exDate(parseLocalDate(fields.get("ex_date")))
                .recordDate(parseLocalDate(fields.get("record_date")))
                .payableDate(parseLocalDate(fields.get("payable_date")))
                .build();
    }

    private static BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate ld) return ld;
        try {
            return LocalDate.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
