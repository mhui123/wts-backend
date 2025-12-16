package com.wts.kiwoom.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "kiwoom_permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_kiwoom_permission_unique",
                        columnNames = {"user_id"})
        })
@Getter
@Setter
public class KiwoomPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level")
    private KiwoomPermissionLevel permissionLevel;

    @Column(name = "daily_request_limit")
    private Integer dailyRequestLimit = 1000;

    @Column(name = "current_request_count")
    private Integer currentRequestCount = 0;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;
}

