package com.wts.summary.jpa.entity;

import com.wts.auth.jpa.entity.User;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_trade_history_unique",
                        columnNames = {"user_id", "trade_date", "trade_type", "symbol_name", "isin", "quantity", "amount_krw", "balance_krw"})
        })
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

    @Column(name = "trade_type", nullable = false, length = 30)
    private String tradeType;

    @Column(name = "symbol_name", length = 150)
    private String symbolName;

    @Column(name = "fx_rate", precision = 38, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "quantity", precision = 38, scale = 6)
    private BigDecimal quantity;

    @Column(name = "amount_krw", precision = 38, scale = 6)
    private BigDecimal amountKrw;

    @Column(name = "amount_usd", precision = 38, scale = 6)
    private BigDecimal amountUsd;

    @Column(name = "price_krw", precision = 38, scale = 6)
    private BigDecimal priceKrw;

    @Column(name = "price_usd", precision = 38, scale = 6)
    private BigDecimal priceUsd;

    @Column(name = "fee_krw", precision = 38, scale = 6)
    private BigDecimal feeKrw;

    @Column(name = "fee_usd", precision = 38, scale = 6)
    private BigDecimal feeUsd;

    @Column(name = "tax_krw", precision = 38, scale = 6)
    private BigDecimal taxKrw;

    @Column(name = "tax_usd", precision = 38, scale = 6)
    private BigDecimal taxUsd;

    @Column(name = "repay_total_krw", precision = 38, scale = 6)
    private BigDecimal repayTotalKrw;

    @Column(name = "repay_total_usd", precision = 38, scale = 6)
    private BigDecimal repayTotalUsd;

    @Column(name = "balance_qty", precision = 38, scale = 6)
    private BigDecimal balanceQty;

    @Column(name = "balance_krw", precision = 38, scale = 6)
    private BigDecimal balanceKrw;

    @Column(name = "balance_usd", precision = 38, scale = 6)
    private BigDecimal balanceUsd;

    @Column(name = "source_row")
    private Integer sourceRow;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "isin", length = 12)
    private String isin;

    @Column(name = "broker_name", length = 50)
    @Enumerated(EnumType.STRING)
    private BrokerType brokerName;

    @Column(name = "trade_currency")
    @Enumerated(EnumType.STRING)
    private Currency tradeCurrency;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

