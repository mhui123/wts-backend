package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.PendingFetchDividend;
import com.wts.summary.jpa.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingFetchDividendRepository extends JpaRepository<PendingFetchDividend, Long>, JpaSpecificationExecutor<TradeHistory> {
    Optional<PendingFetchDividend> findByTicker(String ticker);
}
