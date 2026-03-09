package com.wts.summary.jpa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Table(name = "stock_distributions",
       uniqueConstraints = @UniqueConstraint(
           name = "ux_ticker_declareddate",
           columnNames = {"ticker", "declared_date"}
       ))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDistribution {
    @Id
    @Column(name = "dist_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long distId; // 고유 PK: 반드시 존재해야 함

    @Column(name = "ticker", length = 16)
    private String ticker;
    @Column(name = "distribution_per_share", precision = 16, scale = 6)
    private BigDecimal distributionPerShare;
    @Column(name = "roc_pct", precision = 10, scale = 4)
    private BigDecimal rocPct;
    @Column(name = "declared_date")
    private LocalDate declaredDate;
    @Column(name = "ex_date")
    private LocalDate exDate;
    @Column(name = "record_date")
    private LocalDate recordDate;
    @Column(name = "payable_date")
    private LocalDate payableDate;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
