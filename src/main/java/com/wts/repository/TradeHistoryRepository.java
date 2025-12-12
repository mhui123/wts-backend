package com.wts.repository;

import com.wts.entity.PortfolioItem;
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
    @Query(value=
            "select th.trade_type, th.symbol_name, " +
                    " sum(th.amount_krw ) as total_amount_krw, sum(th.amount_usd) as total_amount_usd, sum(th.quantity) as quantity " +
                    " , sum(th.amount_usd) / sum(th.quantity) as avg_price_krw, sum(th.amount_usd) / sum(th.quantity) as avg_price_usd, th.isin " +
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


    @Query(value=
            "select th.trade_date, th.trade_type, th.symbol_name, th.quantity, th.amount_krw, th.amount_usd, th.price_krw , th.price_usd, " +
                    " th.fee_krw , th.fee_usd , th.tax_krw , th.tax_usd, th.isin " +
                    " from trade_history th " +
                    " where th.user_id = :userId " +
                    " and th.symbol_name = :companyName " +
                    " and th.trade_type in ( :types ) " +
                    " order by th.symbol_name, th.trade_date ", nativeQuery = true)
    List<Object[]> getTradeHistoryBySymbolAndUserIdOrderByTradeDate(Long userId, String companyName, List<String> types);

    List<TradeHistory> findByUserIdAndSymbolNameAndTradeType(Long userId, String symbolName, String tradeType);
}
