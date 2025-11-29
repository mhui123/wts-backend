package com.wts.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tr_hist_id")
    private Long trHistId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "trade_type", nullable = false)
    private String tradeType;

    @Column(name = "symbol_name")
    private String symbolName;

    @Column(name = "fx_rate")
    private BigDecimal fxRate;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "amount_krw")
    private BigDecimal amountKrw;

    @Column(name = "amount_usd")
    private BigDecimal amountUsd;

    @Column(name = "price_krw")
    private BigDecimal priceKrw;

    @Column(name = "price_usd")
    private BigDecimal priceUsd;

    @Column(name = "fee_krw")
    private BigDecimal feeKrw;

    @Column(name = "fee_usd")
    private BigDecimal feeUsd;

    @Column(name = "tax_krw")
    private BigDecimal taxKrw;

    @Column(name = "tax_usd")
    private BigDecimal taxUsd;

    @Column(name = "repay_total_krw")
    private BigDecimal repayTotalKrw;

    @Column(name = "repay_total_usd")
    private BigDecimal repayTotalUsd;

    @Column(name = "balance_qty")
    private BigDecimal balanceQty;

    @Column(name = "balance_krw")
    private BigDecimal balanceKrw;

    @Column(name = "balance_usd")
    private BigDecimal balanceUsd;

    @Column(name = "source_row")
    private Integer sourceRow;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

