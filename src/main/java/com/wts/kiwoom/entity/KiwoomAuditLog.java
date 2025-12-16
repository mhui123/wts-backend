package com.wts.kiwoom.entity;

import io.netty.handler.codec.socksx.v4.Socks4CommandRequest;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Entity
@Table(name = "kiwoom_audit_logs", indexes = {
        @Index(name = "idx_user_timestamp", columnList = "userId, timestamp"),
        @Index(name = "idx_api_endpoint", columnList = "apiEndpoint")
})
@Builder
public class KiwoomAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "error_message")
    private String errorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private KiwoomStatus status; // SUCCESS, ERROR, TIMEOUT

    @Column(name = "execution_time")
    private Long executionTime;
}
