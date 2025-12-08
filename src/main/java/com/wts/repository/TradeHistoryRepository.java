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
                    " , avg(th.price_krw) as avg_price_krw, avg(th.price_usd) as avg_price_usd " +
                    " from stockdb.trade_history th " +
                    " where 1=1" +
                    " and th.user_id = :userId " +
                    " and th.trade_type In (:types) " +
                    " group by th.symbol_name, th.trade_type " +
                    " order by th.symbol_name ", nativeQuery = true)
    List<Object[]> findByUser_IdTotalDiv(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query(value=
            "select th.trade_date, th.trade_type, th.symbol_name, th.quantity, th.amount_krw, th.amount_usd, th.price_krw , th.price_usd, " +
                    " th.fee_krw , th.fee_usd , th.tax_krw , th.tax_usd " +
                    " from trade_history th " +
                    " where th.user_id = :userId " +
                    " and th.symbol_name in( :symbols ) " +
                    " and th.trade_type in ( :types ) " +
                    " order by th.symbol_name, th.trade_date ", nativeQuery = true)
    List<Object[]> getTrList(@Param("userId") Long userId, @Param("symbols") List<String> symbols, @Param("types") List<String> types);

    @Query(value=
            "select th.trade_type, th.symbol_name, " +
                    " sum(th.amount_krw ) as total_amount_krw, sum(th.amount_usd) as total_amount_usd, sum(th.quantity) as quantity " +
                    " , avg(th.price_krw) as avg_price_krw, avg(th.price_usd) as avg_price_usd, th.isin " +
                    " , sum(th.fee_krw) as fee_krw , sum(th.fee_usd) as fee_usd, sum(th.tax_krw) as tax_krw, sum(th.tax_usd) as tax_usd " +
                    " from stockdb.trade_history th " +
                    " where 1=1" +
                    " and th.user_id = :userId " +
                    " and th.trade_type In (:types) " +
                    " group by th.symbol_name, th.trade_type, th.isin " +
                    " order by th.symbol_name ", nativeQuery = true)
    List<Object[]> getGroupedTrList(@Param("userId") Long userId, @Param("types") List<String> types);

    @Query(value=
            "select th.trade_date, th.trade_type, th.symbol_name, th.quantity, th.amount_krw, th.amount_usd, th.price_krw , th.price_usd, " +
                    " th.fee_krw , th.fee_usd , th.tax_krw , th.tax_usd, th.isin " +
                    " from trade_history th " +
                    " where th.user_id = :userId " +
                    " and th.trade_type in ( :types ) " +
                    " order by th.symbol_name, th.trade_date ", nativeQuery = true)
    List<Object[]> getProfitList(@Param("userId") Long userId, @Param("types") List<String> types);
}
