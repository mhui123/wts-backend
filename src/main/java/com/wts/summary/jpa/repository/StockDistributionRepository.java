package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.StockDistribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockDistributionRepository extends JpaRepository<StockDistribution, Long> {
    List<StockDistribution> findByTicker(String ticker);

    // ticker 목록에 해당하는 모든 배당 이력을 한 번에 조회 (N+1 방지)
    @Query("SELECT d FROM StockDistribution d WHERE d.ticker IN :tickers")
    List<StockDistribution> findByTickerIn(@Param("tickers") List<String> tickers);
}

