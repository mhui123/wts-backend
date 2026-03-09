package com.wts.summary.jpa.repository;

import com.wts.summary.jpa.entity.CashflowEntity;
import com.wts.summary.jpa.entity.CashflowDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashFlowDetailRepository extends JpaRepository<CashflowDetailEntity, Long>, JpaSpecificationExecutor<CashflowDetailEntity> {
    List<CashflowDetailEntity> findByCashflowEntity(CashflowEntity cashflowEntity);
    List<CashflowDetailEntity> findByCashflowEntityOrderByItemDateAsc(CashflowEntity cashflowEntity);
}
