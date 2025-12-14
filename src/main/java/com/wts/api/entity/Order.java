// 주문 엔티티: DB에 저장되는 주문 정보를 표현합니다.
// 주요 책임: 주문 속성(심볼, 수량, 가격 등) 보관 및 JPA 매핑
package com.wts.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "wts_orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_order_id", unique = true)
    private String clientOrderId;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String side; // BUY or SELL

    @Column(nullable = false)
    private Long qty;

    private Double price; // nullable for market

    @Column(nullable = false)
    private String status = "NEW";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Order() {}

    public Order(String clientOrderId, String symbol, String side, Long qty, Double price, String status) {
        this.clientOrderId = clientOrderId;
        this.symbol = symbol;
        this.side = side;
        this.qty = qty;
        this.price = price;
        this.status = status;
    }
}
