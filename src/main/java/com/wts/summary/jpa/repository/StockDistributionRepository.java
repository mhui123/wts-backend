package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.StockDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockDistributionRepository extends JpaRepository<StockDistribution, Long> {
    List<StockDistribution> findByTicker(String ticker);
}

