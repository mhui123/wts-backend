package com.wts.summary.jpa.entity;


import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "portfolio_item",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_user_trade_symbol", columnNames = {"user_id", "company_name"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 고유 PK: 반드시 존재해야 함

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "symbol", length = 100)
    private String symbol;

    @Column(name = "quantity", precision = 20, scale = 6)
    private BigDecimal quantity; //보유수량
    @Column(name = "current_price", precision = 19, scale = 4)
    private BigDecimal currentPrice;

    @Column(name = "buy_qty", precision = 20, scale = 6)
    private BigDecimal buyQty; //총 구매수량
    @Column(name = "sell_qty", precision = 20, scale = 6)
    private BigDecimal sellQty;
    @Column(name = "avg_sell_price", precision = 19, scale = 4)
    private BigDecimal avgSellPrice;
    @Column(name = "avg_buy_price", precision = 19, scale = 4)
    private BigDecimal avgBuyPrice;
    @Column(name = "total_sell", precision = 19, scale = 4)
    private BigDecimal totalSell;
    @Column(name = "total_buy", precision = 19, scale = 4)
    private BigDecimal totalBuy;
    @Column(name = "profit", precision = 19, scale = 4)
    private BigDecimal profit;
    @Column(name = "profit_rate", precision = 19, scale = 4)
    private BigDecimal profitRate;
    @Column(name = "dividend", precision = 19, scale = 4)
    private BigDecimal dividend;
    @Column(name = "tax", precision = 19, scale = 4)
    private BigDecimal tax;
    @Column(name = "fee", precision = 19, scale = 4)
    private BigDecimal fee;
    @Column(name = "sector", length = 30)
    private String sector;
    @Column(name = "weight", precision = 7, scale = 4)
    private BigDecimal weight;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 5)
    private Currency currency;
    @Column(name = "holding_amount", precision = 19, scale = 4)
    private BigDecimal holdingAmount;
    @Column(name = "holding_price", precision = 19, scale = 4)
    private BigDecimal holdingPrice;
    @Column(name = "brokerType", length = 50)
    @Enumerated(EnumType.STRING)
    private BrokerType brokerType;




    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
