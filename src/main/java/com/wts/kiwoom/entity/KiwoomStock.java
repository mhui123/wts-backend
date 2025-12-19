package com.wts.kiwoom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "kiwoom_stock_master",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_kiwoom_stock_master_unique",
                        columnNames = {"stock_cd"})
        },indexes = {
                @Index(name = "idx_stock_name", columnList = "stock_nm")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KiwoomStock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "stock_cd", length = 6)
    private String stockCd;
    @Column(name = "stock_nm", length = 100)
    private String stockNm;
    @Column(name = "market", length = 50)
    private String market;
    @Column(name = "active")
    private Boolean active;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
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
