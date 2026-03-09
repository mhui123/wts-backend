package com.wts.summary.jpa.entity;


import com.wts.auth.jpa.entity.User;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.enums.YesNo;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_cashflow_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_cashflow_master", columnNames = {"user_id", "account", "currency", "base_ym"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cf_id")
    private Long cfId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "base_ym", nullable = false)
    private LocalDate baseYm;
    @Column(name = "start_amount", precision = 19, scale = 4 )
    private BigDecimal startAmount;
    @Column(name = "end_amount", precision = 19, scale = 4)
    private BigDecimal endAmount;
    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency; // account와 혼합하여 TOSS-KRW, KB-USD 등으로 구분
    @Column(name = "account", length = 20)
    @Enumerated(EnumType.STRING)
    private BrokerType account;
    @Column(name = "inflow_amount_krw", precision = 19)
    private BigDecimal inflowAmountKrw;
    @Column(name = "outflow_amount_krw", precision = 19)
    private BigDecimal outflowAmountKrw;
    @Column(name = "inflow_amount_usd", precision = 19, scale = 2)
    private BigDecimal inflowAmountUsd;
    @Column(name = "outflow_amount_usd", precision = 19, scale = 2)
    private BigDecimal outflowAmountUsd;
    @Column(name = "net_cashflow_krw", precision = 19)
    private BigDecimal netCashflowKrw;
    @Column(name = "net_cashflow_usd", precision = 19, scale = 2)
    private BigDecimal netCashflowUsd;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "calculate_flag")
    @Enumerated(EnumType.STRING)
    private YesNo calculateFlag;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addInflow(Currency currency, BigDecimal itemAmount) {
        if (currency == Currency.KRW) {
            if(this.inflowAmountKrw == null) {
                this.inflowAmountKrw = BigDecimal.ZERO;
            }
            this.inflowAmountKrw = this.inflowAmountKrw.add(itemAmount);
        } else if (currency == Currency.USD) {
            if(this.inflowAmountUsd == null) {
                this.inflowAmountUsd = BigDecimal.ZERO;
            }
            this.inflowAmountUsd = this.inflowAmountUsd.add(itemAmount);
        }
    }

    public void addOutflow(Currency currency, BigDecimal itemAmount) {
        if (currency == Currency.KRW) {
            if(this.outflowAmountKrw == null) {
                this.outflowAmountKrw = BigDecimal.ZERO;
            }
            this.outflowAmountKrw = this.outflowAmountKrw.add(itemAmount);
        } else if (currency == Currency.USD) {
            if(this.outflowAmountUsd == null) {
                this.outflowAmountUsd = BigDecimal.ZERO;
            }
            this.outflowAmountUsd = this.outflowAmountUsd.add(itemAmount);
        }
    }
}
