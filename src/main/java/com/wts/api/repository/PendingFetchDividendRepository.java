package com.wts.api.repository;

import com.wts.api.entity.PendingFetchDividend;
import com.wts.api.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingFetchDividendRepository extends JpaRepository<PendingFetchDividend, Long>, JpaSpecificationExecutor<TradeHistory> {
    Optional<PendingFetchDividend> findByTicker(String ticker);
}
