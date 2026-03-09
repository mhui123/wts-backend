package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.CashflowEntity;
import com.wts.auth.jpa.entity.User;
import com.wts.summary.enums.Currency;
import com.wts.summary.enums.YesNo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashFlowRepository extends JpaRepository<CashflowEntity, Long>, JpaSpecificationExecutor<CashflowEntity> {
    Optional<CashflowEntity> findByUserAndBaseYmAndCurrency(User user, LocalDate baseDate, Currency currency);

    Optional<CashflowEntity> findByUserAndBaseYmAndCurrencyAndCalculateFlag(User user, LocalDate baseDate, Currency currency, YesNo yesNo);

    List<CashflowEntity> findByUserIdAndCurrencyOrderByBaseYmAsc(Long userId, Currency currency);

    Optional<List<CashflowEntity>> findByUserIdAndBaseYmBetween(Long userId, LocalDate startDate, LocalDate endDate);

    Optional<CashflowEntity> findByUserIdAndBaseYmAndCurrency(Long userId, LocalDate baseYm, Currency currency);

    Optional<List<CashflowEntity>> findByUserIdAndCurrencyAndCalculateFlag(Long userId, Currency currency, YesNo yesNo);
}
