package com.wts.summary.jpa.entity;

import com.wts.summary.enums.FlowType;
import com.wts.summary.enums.InOut;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "monthly_cashflow_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashflowDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cf_id")
    private CashflowEntity cashflowEntity;
    @Column(name="main_category", length = 20)
    @Enumerated(EnumType.STRING)
    private InOut mainCategory;
    @Column(name = "flow_type", length = 20)
    @Enumerated(EnumType.STRING)
    private FlowType flowType;
    @Column(name = "item_date", nullable = false)
    private LocalDate itemDate;
    @Column(name = "item_name", length = 50)
    private String itemName;
    @Column(name = "item_amount_krw", precision = 19, scale = 2 )
    private BigDecimal itemAmountKrw;
    @Column(name = "item_amount_usd", precision = 19, scale = 2 )
    private BigDecimal itemAmountUsd;
    @Column(name = "fxRate", precision = 10, scale = 2 )
    private BigDecimal fxRate;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "balance_krw", precision = 19, scale = 2 )
    private BigDecimal balanceKrw;
    @Column(name = "balance_usd", precision = 19, scale = 2 )
    private BigDecimal balanceUsd;

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
