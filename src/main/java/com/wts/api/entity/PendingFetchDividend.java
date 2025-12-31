package com.wts.api.entity;

import com.wts.api.enums.YesNo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_distributions_table",
        uniqueConstraints = {
                @UniqueConstraint(name = "pending_dividend_history_unique",
                        columnNames = {"ticker"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingFetchDividend {
    @Id
    @Column(name = "pending_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pendingId; // 고유 PK: 반드시 존재해야 함

    @Column(name = "ticker", length = 16)
    private String ticker;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 1, nullable = false)
    private YesNo status = YesNo.N;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
