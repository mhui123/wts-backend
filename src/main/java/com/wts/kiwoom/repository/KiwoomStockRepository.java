package com.wts.kiwoom.repository;

import com.wts.kiwoom.entity.KiwoomStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KiwoomStockRepository extends JpaRepository<KiwoomStock, Long> {
    Optional<KiwoomStock> findByStockCd(String stockCode);
}