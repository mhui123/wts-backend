package com.wts.repository;

import com.wts.entity.DashboardDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardDetailRepository extends JpaRepository<DashboardDetail, Long> {
    List<DashboardDetail> findByUserId(Long userId);

    Optional<DashboardDetail> findByUserIdAndTradeTypeAndSymbolName(Long userId, String tradeType, String symbolName);
}

