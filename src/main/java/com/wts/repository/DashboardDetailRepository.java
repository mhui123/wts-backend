package com.wts.repository;

import com.wts.entity.DashboardDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DashboardDetailRepository extends JpaRepository<DashboardDetail, Long> {
    List<DashboardDetail> findByUserId(Long userId);

    Optional<DashboardDetail> findByUserIdAndTradeTypeAndSymbolName(Long userId, String tradeType, String symbolName);

    @Query(value=
            "SELECT " +
                    "    symbol_name," +
                    "    SUM(CASE WHEN trade_type = '판매' THEN total_amount_krw ELSE 0 END) + SUM(CASE WHEN trade_type = '구매' THEN total_amount_krw * -1 ELSE 0 END) AS profit_loss_krw, " +
                    "    SUM(CASE WHEN trade_type = '판매' THEN total_amount_usd ELSE 0 END) + SUM(CASE WHEN trade_type = '구매' THEN total_amount_usd * -1 ELSE 0 END) AS profit_loss_usd " +
                    " FROM dashboard_detail " +
                    " WHERE user_id = :userId " +
                    "  AND trade_type IN ( :types ) " +
                    " GROUP BY symbol_name " +
                    " HAVING COUNT(DISTINCT trade_type) = 2 ", nativeQuery = true)
    List<Object[]> findByUser_IdProfits(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query(value=
            "SELECT " +
                    "    symbol_name " +
                    " FROM dashboard_detail " +
                    " WHERE user_id = :userId " +
                    "  AND trade_type IN ( :types ) " +
                    " GROUP BY symbol_name " +
                    " HAVING COUNT(DISTINCT trade_type) = 2 ", nativeQuery = true)
    List<String> findNeedCalProfitSymbols(@Param("userId") Long userId , @Param("types") List<String> types);
}

