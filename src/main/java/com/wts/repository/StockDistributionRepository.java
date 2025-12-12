package com.wts.repository;

import com.wts.entity.StockDistribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockDistributionRepository extends JpaRepository<StockDistribution, Long> {
    List<StockDistribution> findByTicker(String ticker);
}

