package com.wts.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_info")
@Getter
@Setter
public class DashboardInfo {

    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_investment_krw", precision = 19, scale = 2)
    private BigDecimal totalInvestmentKrw = BigDecimal.ZERO;

    @Column(name = "total_investment_usd", precision = 19, scale = 2)
    private BigDecimal totalInvestmentUsd = BigDecimal.ZERO;

    @Column(name = "total_dividend_krw", precision = 19, scale = 2)
    private BigDecimal totalDividendKrw = BigDecimal.ZERO;
    @Column(name = "total_dividend_usd", precision = 19, scale = 2)
    private BigDecimal totalDividendUsd = BigDecimal.ZERO;

    @Column(name = "total_profit_loss_krw", precision = 19, scale = 2)
    private BigDecimal totalProfitLossKrw = BigDecimal.ZERO;
    @Column(name = "total_profit_loss_usd", precision = 19, scale = 2)
    private BigDecimal totalProfitLossUsd = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DashboardInfo() {}

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

