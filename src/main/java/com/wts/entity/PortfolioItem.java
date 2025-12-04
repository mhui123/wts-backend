package com.wts.entity;

import com.wts.model.PortfolioItemDto;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "portfolio_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_user_trade_symbol", columnNames = {"user_id", "company_name"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 PK: 반드시 존재해야 함

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_name", length = 255, nullable = false)
    private String companyName;

    @Column(name = "symbol", length = 100)
    private String symbol;

    // quantity
    @Column(name = "quantity", precision = 20, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "avg_price_usd", precision = 19, scale = 4)
    private BigDecimal avgPriceUsd;
    @Column(name = "avg_price_krw", precision = 19, scale = 4)
    private BigDecimal avgPriceKrw;
    @Column(name = "current_price_usd", precision = 19, scale = 4)
    private BigDecimal currentPriceUsd;
    @Column(name = "current_price_krw", precision = 19, scale = 4)
    private BigDecimal currentPriceKrw;
    @Column(name = "total_value_usd", precision = 19, scale = 4)
    private BigDecimal totalValueUsd;
    @Column(name = "total_value_krw", precision = 19, scale = 4)
    private BigDecimal totalValueKrw;

    @Column(name = "total_investment_krw", precision = 19, scale = 4)
    private BigDecimal totalInvestmentKrw;
    @Column(name = "total_investment_usd", precision = 19, scale = 4)
    private BigDecimal totalInvestmentUsd;

    @Column(name = "profit_usd", precision = 19, scale = 4)
    private BigDecimal profitUsd;

    @Column(name = "profit_krw", precision = 19, scale = 4)
    private BigDecimal profitKrw;

    @Column(name = "profit_rate_usd", precision = 19, scale = 4)
    private BigDecimal profitRateUsd;

    @Column(name = "profit_rate_krw", precision = 19, scale = 4)
    private BigDecimal profitRateKrw;

    @Column(name = "dividend_usd", precision = 19, scale = 4)
    private BigDecimal dividendUsd;

    @Column(name = "dividend_krw", precision = 19, scale = 4)
    private BigDecimal dividendKrw;

    @Column(name = "sector", length = 30)
    private String sector;
    @Column(name = "weight", precision = 7, scale = 4)
    private BigDecimal weight;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static PortfolioItem fromDto(PortfolioItemDto p) {
        return PortfolioItem.builder()
                .userId(p.getUserId())
                .companyName(p.getCompanyName())
                .symbol(p.getSymbol())
                .quantity(p.getQuantity())
                .avgPriceUsd(p.getAvgPriceUsd())
                .avgPriceKrw(p.getAvgPriceKrw())
                .currentPriceUsd(p.getCurrentPriceUsd())
                .currentPriceKrw(p.getCurrentPriceKrw())
                .totalValueUsd(p.getTotalValueUsd())
                .totalValueKrw(p.getTotalValueKrw())
                .profitUsd(p.getProfitUsd())
                .profitKrw(p.getProfitKrw())
                .profitRateUsd(p.getProfitRateUsd())
                .profitRateKrw(p.getProfitRateKrw())
                .dividendUsd(p.getDividendUsd())
                .dividendKrw(p.getDividendKrw())
                .sector(p.getSector())
                .weight(p.getWeight())
                .build();
    }


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
