package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.KiwoomAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KiwoomAuditRepository extends JpaRepository<KiwoomAuditLog, Long> {
}

