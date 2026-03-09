package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long>, JpaSpecificationExecutor<TradeHistory> {

    List<TradeHistory> findByUserIdOrderByTradeDateAsc(Long userId);

}
