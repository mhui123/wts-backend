package com.wts.auth.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_permissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_user_permission_unique",
                        columnNames = {"user_id"})
        })
@Getter
@Setter
public class UserPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level")
    private UserPermissionLevel permissionLevel;

    @Column(name = "daily_request_limit")
    private Integer dailyRequestLimit = 1000;

    @Column(name = "current_request_count")
    private Integer currentRequestCount = 0;

    @Column(name = "last_reset_date")
    private LocalDate lastResetDate;

    public enum UserPermissionLevel {
        BASIC_USER,     // 조회만
        TRADING_USER,   // 거래 가능
        ADMIN_USER      // 관리자
    }
}

