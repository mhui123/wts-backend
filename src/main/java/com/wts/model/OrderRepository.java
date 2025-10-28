// 주문 저장소: JPA를 사용해 Order 엔티티의 CRUD 및 클라이언트 주문 ID로 조회 기능을 제공합니다.
package com.wts.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByClientOrderId(String clientOrderId);
}
