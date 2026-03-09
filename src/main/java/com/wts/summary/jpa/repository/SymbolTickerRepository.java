package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.SymbolTicker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SymbolTickerRepository extends JpaRepository<SymbolTicker, Long> {
    Optional<SymbolTicker> findByIsin(String isin);
    Optional<SymbolTicker> findByTicker(String ticker);
    Optional<List<SymbolTicker>> findByTickerIsNull();
    Optional<SymbolTicker> findBySymbolName(String symbolName);
}