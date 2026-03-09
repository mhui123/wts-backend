package com.wts.util;

import com.wts.model.Money;
import com.wts.model.PortfolioPositionVO;
import com.wts.model.Quantity;
import com.wts.model.TradeHistoryVO;
import com.wts.summary.enums.Currency;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PortfolioCalculator {
    public static <T> PortfolioPositionVO calculate(
            List<TradeHistoryVO> trades, Currency c) {
        Money totalBuy = Money.zero(c);
        Money totalSell = Money.zero(c);
        Quantity buyQty = Quantity.zero();
        Quantity sellQty = Quantity.zero();
        Quantity holdingQty = Quantity.zero();
        BigDecimal outQty = BigDecimal.ZERO;
        BigDecimal inQty;
        BigDecimal inoutRate;
        Money profit = Money.zero(c);
        Money dividend = Money.zero(c);
        Money avgBuyPrice = Money.zero(c);
        Money avgSellPrice;
        Money holdingAmount = Money.zero(c);
        Money holdingPrice = Money.zero(c);

        Money fee = Money.zero(c);
        Money tax = Money.zero(c);

        boolean inoutFlag = false; //입출고 플래그
        LocalDate lastOutDate = null;
        List<TradeHistoryVO> betweenSplitTrades = new ArrayList<>();

        for (TradeHistoryVO t : trades) {
            if (t.tradeType().isBuy()) {
                totalBuy = totalBuy.add(t.amount());
                holdingAmount = holdingAmount.add(t.amount());
                buyQty = buyQty.add(t.quantity());
                holdingQty = holdingQty.add(t.quantity());
                avgBuyPrice = getAvgPrice(holdingQty, holdingAmount);
                fee = fee.add(t.fee());
                tax = tax.add(t.tax());

//                if(t.symbolName().equals("디파이언스 나스닥100 옵션 배당 ETF")){
//                    log.debug("[{}][{}][매수] {} / {} = {}  | 매수수량 : {} | 보유수량 : {}",
//                            t.symbolName(), t.tradeDate(), holdingAmount.amount(), holdingQty.value(), avgBuyPrice.amount(), buyQty.value(), holdingQty.value());
//                }

                //최근 30일 이내 분할병합 이력이 있을경우, 매수내역을 별도 보관
                if(lastOutDate != null && compareDateIn30(t.tradeDate(), lastOutDate)){
                    betweenSplitTrades.add(t);
                }


            } else if (t.tradeType().isSell()) {
                Money sellAmount = Money.zero(c).add(t.amount());
                totalSell = totalSell.add(sellAmount);
                sellQty = sellQty.add(t.quantity());
                holdingQty = holdingQty.subtract(t.quantity());
                avgSellPrice = getAvgPrice(t.quantity(), sellAmount);

                //전량매도 발생시, 보유금액 초기화
                if(holdingQty.isZero()) {
                    holdingAmount = Money.zero(c);
                } else {
                    holdingAmount = holdingAmount.subtract(avgBuyPrice.totalAmount(t.quantity())); //구매단가 기준으로 판매개수만큼 매입금에서 차감
                }

                profit = profit.add(
                        avgSellPrice.subtract(avgBuyPrice).totalAmount(t.quantity()).subtract(t.fee()).subtract(t.tax())
                        );

//                if(t.symbolName().equals("디파이언스 나스닥100 옵션 배당 ETF")){
//                    log.info("[{}][{}][매도] {} * {} = {} | 구매단가 : {} | 누적손익 : {} | 매입금 : {}", t.symbolName(), t.tradeDate(), avgSellPrice.amount(), t.quantity().value(), sellAmount.amount(), avgBuyPrice.amount(), profit.amount(), holdingAmount.amount());
//                }

                //최근 30일 이내 분할병합 이력이 있을경우, 매수내역을 별도 보관
                if(lastOutDate != null && compareDateIn30(t.tradeDate(), lastOutDate)){
                    betweenSplitTrades.add(t);
                }

            } else if (t.tradeType().isStockOut()){
                outQty = t.quantity().value();
                holdingQty = !holdingQty.isZero() ? holdingQty.subtract(t.quantity()) : Quantity.zero();
                log.debug("{} 출고 {} 개", t.symbolName(), t.quantity());
                if(lastOutDate != null){
                    if(compareDateOver30(t.tradeDate(), lastOutDate)) {
                        lastOutDate = t.tradeDate();
                        inoutFlag = true;
                    }
                } else {
                    lastOutDate = t.tradeDate();
                    inoutFlag = true;
                }
            } else if (t.tradeType().isStockIn()) {
                inQty = t.quantity().value();
                holdingQty = holdingQty.isZero() ? t.quantity() : holdingQty.add(t.quantity());

                log.debug("{} 입고 {} 개", t.symbolName(), t.quantity());
                if(outQty.compareTo(BigDecimal.ZERO) != 0 && inoutFlag) {
                    inoutRate = inQty.divide(outQty, 8, RoundingMode.HALF_UP); //병합 분할 비율
                    //병합분할 비율에 따른 매수 매도 수량, 단가 조정
                    buyQty = new Quantity(buyQty.value().multiply(inoutRate).setScale(0, RoundingMode.HALF_UP));
                    sellQty = new Quantity(sellQty.value().multiply(inoutRate).setScale(0, RoundingMode.HALF_UP));
                    avgBuyPrice = new Money(
                            avgBuyPrice.amount().divide(inoutRate, 8, RoundingMode.HALF_UP),
                            avgBuyPrice.currency()
                    );

                    log.debug("{} [입고]병합분할비율 : {} . buy : {} sell : {} holding : {}", t.symbolName(), inoutRate, buyQty.value(), sellQty.value(), holdingQty.value());

                    inQty = BigDecimal.ZERO;
                    outQty = BigDecimal.ZERO;

                    inoutFlag = false;
                } else if(outQty.compareTo(BigDecimal.ZERO) != 0 && !inoutFlag){
                    inoutRate = inQty.divide(outQty, 8, RoundingMode.HALF_UP); //병합 분할 비율
                    // 임시저장한 거래내역들의 처리
                    for(TradeHistoryVO bt : betweenSplitTrades){
                        if(bt.tradeType().isBuy()){
                            //조정된 수량으로 재계산
                            Quantity qty = bt.quantity();
                            Quantity adjustedQty = new Quantity(qty.value().multiply(inoutRate).setScale(2, RoundingMode.HALF_UP));
                            buyQty = buyQty.subtract(qty).add(adjustedQty);
                            log.debug("[{}] {} [매수조정] - {} + {} = {} ", bt.tradeDate(), bt.symbolName(), qty.value(), adjustedQty.value(), buyQty.value());
                        } else if(bt.tradeType().isSell()){
                            //조정된 수량으로 재계산
                            Quantity qty = bt.quantity();
                            Quantity adjustedQty = new Quantity(qty.value().multiply(inoutRate).setScale(2, RoundingMode.HALF_UP));
                            buyQty = sellQty.subtract(qty).add(adjustedQty);
                            log.debug("[{}] {} [매수조정] - {} + {} = {} ", bt.tradeDate(), bt.symbolName(), qty.value(), adjustedQty.value(), sellQty.value());
                        }
                    }
                    betweenSplitTrades.clear();
                }
            } else if (t.tradeType().isDividend()) {
                dividend = dividend.add(t.amount());
            } else if (t.tradeType().isDividendCancel()) {
                dividend = dividend.subtract(t.amount());
            }
        }
        log.debug("{} 구매수량 : {}, 판매수량: {}, 보유수량: {}, 총 구매: {}, 총 판매: {}, 손익 : {}, 배당: {}",
                trades.get(0).symbolName(),
                buyQty,
                sellQty,
                holdingQty,
                totalBuy,
                totalSell,
                profit,
                dividend
        );

        if(!holdingQty.isZero() && holdingAmount.amount().compareTo(BigDecimal.ZERO) != 0) {
            holdingPrice = new Money(holdingAmount.amount().divide(holdingQty.value(), 8, RoundingMode.HALF_UP), c);
        }
        holdingAmount = holdingQty.isZero() || holdingQty.isNegative() ? Money.zero(c) : holdingAmount;
        holdingQty = holdingQty.isZero() || holdingQty.isNegative() ? Quantity.zero() : holdingQty;

        return new PortfolioPositionVO(
                buyQty,
                sellQty,
                holdingQty,
                totalBuy,
                totalSell,
                profit,
                dividend,
                holdingAmount,
                holdingPrice
        );
    }

    private static Money getAvgPrice(Quantity qty, Money total) {
        if (qty.isZero()) {
            return Money.zero(total.currency());
        }
        return new Money(
                total.amount().divide(qty.value(), 8, RoundingMode.HALF_UP),
                total.currency()
        );
    }

    private static boolean compareDateOver30(LocalDate d1, LocalDate d2) {
        long dayDiff = ChronoUnit.DAYS.between(d1, d2);
        return dayDiff > 30L;
    }

    private static boolean compareDateIn30(LocalDate d1, LocalDate d2) {
        long dayDiff = ChronoUnit.DAYS.between(d1, d2);
        return dayDiff <= 30L;
    }
}
