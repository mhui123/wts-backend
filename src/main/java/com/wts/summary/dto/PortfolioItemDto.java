package com.wts.summary.dto;

import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortfolioItemDto {
    //portfolio_item
    private String companyName;
    private Long userId;
    private BigDecimal buyQty;
    private BigDecimal sellQty;
    private Currency currency;
    private BigDecimal avgBuyPrice;
    private BigDecimal avgSellPrice;
    private BigDecimal totalSell;
    private BigDecimal totalBuy;
    private BigDecimal profit;
    private BigDecimal quantity; //보유수량
    private BigDecimal currentPrice;
    private BigDecimal dividend;
    private BigDecimal fee;
    private BigDecimal tax;
    private String symbol; //ticker.
    private String isin;
    private BigDecimal holdingAmount;
    private BigDecimal holdingPrice;
    private BrokerType brokerType;

}

