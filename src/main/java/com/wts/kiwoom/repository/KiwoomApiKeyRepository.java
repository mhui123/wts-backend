package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.KiwoomApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KiwoomApiKeyRepository extends JpaRepository<KiwoomApiKey, Long> {
    Optional<KiwoomApiKey> findByUserId(Long userId);
    Optional<KiwoomApiKey> findByUserIdAndIsActive(Long userId, Boolean isActive);
}

