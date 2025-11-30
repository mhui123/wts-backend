package com.wts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_detail", uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "trade_type", "symbol_name"})})
@Getter
@Setter
public class DashboardDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 PK: 반드시 존재해야 함

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "symbol_name", length = 255, nullable = false, unique = true)
    private String symbolName;

    @Column(name = "ticker", length = 100)
    private String ticker;

    @Column(name = "trade_type", length = 20)
    private String tradeType;

    // quantity
    @Column(name = "quantity", precision = 20, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    // avg price -> krw / usd
    @Column(name = "avg_price_krw", precision = 19, scale = 4)
    private BigDecimal avgPriceKrw = BigDecimal.ZERO;

    @Column(name = "avg_price_usd", precision = 19, scale = 4)
    private BigDecimal avgPriceUsd = BigDecimal.ZERO;

    // total amount -> krw / usd
    @Column(name = "total_amount_krw", precision = 19, scale = 2)
    private BigDecimal totalAmountKrw = BigDecimal.ZERO;

    @Column(name = "total_amount_usd", precision = 19, scale = 4)
    private BigDecimal totalAmountUsd = BigDecimal.ZERO;

    // profit/loss -> krw / usd
    @Column(name = "profit_loss_krw", precision = 19, scale = 2)
    private BigDecimal profitLossKrw = BigDecimal.ZERO;

    @Column(name = "profit_loss_usd", precision = 19, scale = 4)
    private BigDecimal profitLossUsd = BigDecimal.ZERO;

    // dividend -> krw / usd
    @Column(name = "dividend_krw", precision = 19, scale = 2)
    private BigDecimal dividendKrw = BigDecimal.ZERO;

    @Column(name = "dividend_usd", precision = 19, scale = 4)
    private BigDecimal dividendUsd = BigDecimal.ZERO;

    // current price -> krw / usd
    @Column(name = "current_price_krw", precision = 19, scale = 4)
    private BigDecimal currentPriceKrw = BigDecimal.ZERO;

    @Column(name = "current_price_usd", precision = 19, scale = 4)
    private BigDecimal currentPriceUsd = BigDecimal.ZERO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public DashboardDetail() {}

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
