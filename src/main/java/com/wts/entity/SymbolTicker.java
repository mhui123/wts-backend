package com.wts.entity;

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

    // DB가 자동으로 채우는 TIMESTAMP 값을 읽기전용으로 매핑
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
