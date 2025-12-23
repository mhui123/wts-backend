package com.wts.kiwoom.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_watch_list_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_user_watch_list_item_unique",
                        columnNames = {"group_id", "stock_cd"})
        },
        indexes = {
                @Index(name = "idx_user_watch_list_item_group_id", columnList = "group_id"),
                @Index(name = "idx_user_watch_list_item_stock_cd", columnList = "stock_cd")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserWatchListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private UserWatchGroup watchGroup;

    @Column(name = "stock_cd", nullable = false, length = 6)
    private String stockCd;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}