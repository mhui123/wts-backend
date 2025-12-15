package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.KiwoomApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KiwoomApiKeyRepository extends JpaRepository<KiwoomApiKey, Long> {
    KiwoomApiKey findByUserId(Long userId);
}

