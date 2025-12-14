package com.wts.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "symbol_ticker",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_symbol_isin", columnNames = {"isin"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymbolTicker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "st_id")
    private Long stId;

    @Column(name = "isin", length = 255)
    private String isin;

    @Column(name = "symbol_name", length = 255)
    private String symbolName;

    @Column(name = "ticker", length = 50)
    private String ticker;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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
