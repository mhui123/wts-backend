package com.wts.repository;

import com.wts.entity.SymbolTicker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SymbolTickerRepository extends JpaRepository<SymbolTicker, Long> {
    Optional<SymbolTicker> findByIsin(String isin);
    Optional<SymbolTicker> findByTicker(String ticker);
}