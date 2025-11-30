package com.wts.repository;

import com.wts.entity.DashboardInfo;
import com.wts.model.SummaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DashboardInfoRepository extends JpaRepository<DashboardInfo, Long> {
    Optional<DashboardInfo> findByUserId(Long userId);

    @Query("SELECT new com.wts.model.SummaryRecord(COALESCE(SUM(t.amountKrw), 0), COALESCE(SUM(t.amountUsd), 0)) " +
            "FROM TradeHistory t WHERE t.tradeType = :type")
    SummaryRecord sumAmountUsdByTradeType(@Param("type") String type);

    @Query("SELECT new com.wts.model.SummaryRecord(COALESCE(SUM(t.amountKrw), 0), COALESCE(SUM(t.amountUsd), 0)) " +
            "FROM TradeHistory t WHERE t.tradeType = :type AND t.user.id = :userId")
    SummaryRecord sumAmountByTradeTypeAndUserId(@Param("type") String type, @Param("userId") Long userId);
}

