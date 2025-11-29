package com.wts.repository;

import com.wts.entity.TradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeHistoryRepository extends JpaRepository<TradeHistory, Long> {
    // 추가적인 쿼리 메서드는 서비스에서 EntityManager를 사용해 동적으로 처리합니다.
}

