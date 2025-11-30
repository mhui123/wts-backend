package com.wts.repository;

import com.wts.entity.TradeHistory;
import com.wts.model.SummaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long>, JpaSpecificationExecutor<TradeHistory> {
    @Query("SELECT new com.wts.model.SummaryRecord(COALESCE(SUM(t.amountKrw), 0), COALESCE(SUM(t.amountUsd), 0)) " +
            "FROM TradeHistory t WHERE t.tradeType = :type")
    SummaryRecord sumAmountUsdByTradeType(@Param("type") String type);

    @Query("SELECT new com.wts.model.SummaryRecord(COALESCE(SUM(t.amountKrw), 0), COALESCE(SUM(t.amountUsd), 0)) " +
            "FROM TradeHistory t WHERE t.tradeType = :type AND t.user.id = :userId")
    SummaryRecord sumAmountByTradeTypeAndUserId(@Param("type") String type, @Param("userId") Long userId);

    @Query("SELECT DISTINCT t.symbolName FROM TradeHistory t WHERE t.user.id = :userId AND t.tradeType IN :types")
    List<String> findDistinctSymbolNameByUserIdAndTradeTypeIn(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query("SELECT t FROM TradeHistory t " +
            "WHERE t.user.id = :userId " +
            "AND t.symbolName LIKE CONCAT('%', :symbolName, '%') " +
            "AND t.tradeType IN :types " +
            "ORDER BY t.tradeDate DESC, t.trHistId DESC")
    Optional<TradeHistory> findFirstByUser_IdAndSymbolNameContainingAndTradeTypeInOrderByTradeDateDescTrHistIdDesc(
            Long userId, String symbolName, List<String> types);

    @Query(value=
            "select th.trade_type, th.symbol_name, " +
                    " sum(th.amount_krw ) as total_amount_krw, sum(th.amount_usd) as total_amount_usd, sum(th.quantity) as quantity " +
                    " from stockdb.trade_history th " +
                    " where 1=1" +
                    " and th.user_id = :userId " +
                    " and th.trade_type In (:types) " +
                    " group by th.symbol_name, th.trade_type " +
                    " order by th.symbol_name ", nativeQuery = true)
    List<Object[]> findByUser_IdTotalDiv(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query(value =
            "SELECT symbolName, MAX(quantity) AS quantity, MAX(sum_k) AS sumK, MAX(sum_u) AS sumU FROM ( " +
                    "  ( " +
                    "    SELECT th.symbol_name AS symbolName, th.balance_qty AS quantity, NULL AS sum_k, NULL AS sum_u " +
                    "    FROM stockdb.trade_history th " +
                    "    WHERE th.user_id = :userId " +
                    "    AND th.symbol_name = :symbolPattern   "+
                    "      AND th.trade_type IN (:types) " +
                    "    ORDER BY th.trade_date DESC, th.tr_hist_id DESC " +
                    "    LIMIT 1 " +
                    "  ) " +
                    "  UNION ALL " +
                    "  ( " +
                    "    SELECT th.symbol_name AS symbolName, NULL AS quantity, SUM(th.amount_krw) AS sum_k, SUM(th.amount_usd) AS sum_u " +
                    "    FROM stockdb.trade_history th " +
                    "    WHERE th.user_id = :userId " +
                    "      AND th.symbol_name = :symbolPattern " +
                    "      AND th.trade_type LIKE :dividendPattern " +
                    "    GROUP BY th.symbol_name " +
                    "  ) " +
                    ") T " +
                    "GROUP BY symbolName",
            nativeQuery = true)
    List<com.wts.model.SymbolAggregation> findCombinedByUserIdAndSymbolPatternAndTypes(
            @Param("userId") Long userId,
            @Param("symbolPattern") String symbolPattern,
            @Param("types") List<String> types,
            @Param("dividendPattern") String dividendPattern
    );


}
