package com.wts.kiwoom.repository;


import com.wts.kiwoom.entity.KiwoomPermission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KiwoomPermissionRepository extends JpaRepository<KiwoomPermission, Long> {
    KiwoomPermission findByUserId(Long userId);
}

