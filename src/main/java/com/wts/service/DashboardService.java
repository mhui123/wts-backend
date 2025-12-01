package com.wts.service;

import com.wts.entity.DashboardDetail;
import com.wts.entity.DashboardInfo;
import com.wts.entity.TradeHistory;
import com.wts.model.*;
import com.wts.repository.DashboardDetailRepository;
import com.wts.repository.DashboardInfoRepository;
import com.wts.repository.TradeHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = false)
public class DashboardService {

    @PersistenceContext
    private EntityManager em;

    private final TradeHistoryRepository thRepository;
    private final DashboardInfoRepository dashboardInfoRepository;
    private final DashboardDetailRepository dashboardDetailRepository;
    @Autowired
    private DashboardDetailService dashboardDetailService;

    public DashboardService(DashboardInfoRepository d, DashboardDetailRepository dashboardDetailRepository, TradeHistoryRepository thRepository) {
        this.dashboardInfoRepository = d;
        this.dashboardDetailRepository = dashboardDetailRepository;
        this.thRepository = thRepository;
    }
    public DashboardSummaryDto getCardSummaryInfo(Long userId){
        //기존 데이터 조회
        Optional<DashboardInfo> dashInfo = dashboardInfoRepository.findByUserId(userId);
        if(dashInfo.isPresent()) {
            DashboardInfo info = dashInfo.get();
            List<DashboardDetail> dList = dashboardDetailRepository.findByUserId(userId);
            if(dList.isEmpty()){
                dList = calculateDetail(info);
            }
            return DashboardSummaryDto.builder()
                    .totalInvestmentKrw(info.getTotalInvestmentKrw())
                    .totalInvestmentUsd(info.getTotalInvestmentUsd())
                    .totalDividendKrw(info.getTotalDividendKrw())
                    .totalDividendUsd(info.getTotalDividendUsd())
                    .totalProfitKrw(info.getTotalProfitLossKrw())
                    .totalProfitUsd(info.getTotalProfitLossUsd())
                    .detailList(dList)
                    .build();
        } else {
            return setDataIntoDashboardInfoAndReturnData(userId);
        }
    }

    public DashboardSummaryDto setDataIntoDashboardInfoAndReturnData(Long userId){
        //데이터가 없으므로 거래내역에서 데이터를 처리해 dashboard_info에 입력 후 화면단으로 전달.
        DashboardSummaryDto dto = takeFromHistory(userId);
        DashboardInfo newInfo = new DashboardInfo();
        newInfo.setUserId(userId);
        newInfo.setTotalInvestmentKrw(dto.getTotalInvestmentKrw());
        newInfo.setTotalInvestmentUsd(dto.getTotalInvestmentUsd());

        List<DashboardDetail> dList = calculateDetail(newInfo);

        newInfo.setTotalProfitLossKrw(BigDecimal.ZERO);
        newInfo.setTotalProfitLossUsd(BigDecimal.ZERO);
        dashboardInfoRepository.save(newInfo);

        dto.setTotalDividendUsd(newInfo.getTotalDividendUsd());
        dto.setTotalDividendKrw(newInfo.getTotalDividendKrw());
        dto.setDetailList(dList);
        return dto;
    }

    public DashboardSummaryDto attachDetailList(DashboardSummaryDto dto){
        List<Optional<DashboardStockDto>> dtoList = new ArrayList<>();
        return dto;
    }

    private DashboardSummaryDto takeFromHistory(Long userId){
        String type = "환전외화입금";
        SummaryRecord r;
        BigDecimal totalUsd;
        BigDecimal totalKrw;
        if (userId != null) {
            r = dashboardInfoRepository.sumAmountByTradeTypeAndUserId(type, userId);
        } else {
            r = dashboardInfoRepository.sumAmountUsdByTradeType(type);
        }
        if (r == null) {
            totalUsd = BigDecimal.ZERO;
            totalKrw = BigDecimal.ZERO;
        } else {
            totalUsd = r.sumUsd() != null ? r.sumUsd() : BigDecimal.ZERO;
            totalKrw = r.sumKrw() != null ? r.sumKrw() : BigDecimal.ZERO;
        }
        return DashboardSummaryDto.builder()
                .totalInvestmentUsd(totalUsd)
                .totalInvestmentKrw(totalKrw)
                .build();
    }

    private List<DashboardDetail> calculateDetail(DashboardInfo info){
        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalKrw = BigDecimal.ZERO;
        Long userId = info.getUserId();

        //trade_history로부터 데이터를 얻어와서 계산.
        List<String> types = List.of("외화증권배당금입금", "구매", "판매");
        List<Object[]> rawList = thRepository.findByUser_IdTotalDiv(userId, types);
        List<DashboardDetail> dList = new ArrayList<>(convertToDetailList(rawList));//convertToDetailList(rawList);


        Map<String, BigDecimal> netBySymbol = calcNetBySymbolStream(dList);

        for (int i = 0; i < dList.size(); i++) {
            DashboardDetail d = dList.get(i);
            String tradeType = d.getTradeType();

            if ("외화증권배당금입금".equals(tradeType)) {
                BigDecimal divK = d.getTotalAmountKrw();
                BigDecimal divU = d.getTotalAmountUsd();
                d.setDividendKrw(divK);
                d.setDividendUsd(divU);
                d.setTotalAmountKrw(BigDecimal.ZERO);
                d.setTotalAmountUsd(BigDecimal.ZERO);

                String symbol = d.getSymbolName();
                BigDecimal netQuantity = netBySymbol.getOrDefault(symbol, BigDecimal.ZERO);
                d.setQuantity(netQuantity);
            }

            d.setUserId(userId);

            // 한 번만 저장하고, 반환된 영속 엔티티로 리스트를 교체
            DashboardDetail saved = dashboardDetailService.saveOrUpdate(d);
            if (saved != null) {
                dList.set(i, saved);
            }

            // 배당 합계는 저장된 객체의 값으로 집계
            if ("외화증권배당금입금".equals(tradeType)) {
                totalUsd = totalUsd.add(saved != null && saved.getDividendUsd() != null ? saved.getDividendUsd() : BigDecimal.ZERO);
                totalKrw = totalKrw.add(saved != null && saved.getDividendKrw() != null ? saved.getDividendKrw() : BigDecimal.ZERO);
            }
        }

        info.setTotalDividendUsd(totalUsd);
        info.setTotalDividendKrw(totalKrw);

        return dList;
    }

    public void calDetailProfit(Long userId){
        List<String> symbols = dashboardDetailRepository.findNeedCalProfitSymbols(userId, List.of("구매", "판매"));
        List<Object[]> rawList = thRepository.getTrList(userId, symbols, List.of("구매", "판매"));
        List<TradeHistory> tList = new ArrayList<>(rawList.stream().map(r -> {
                        LocalDate trDate = r[0] != null ? LocalDate.parse(r[0].toString()) : null;
                        String tradeType = r[1] != null ? r[1].toString() : null;
                        String symbolName = r[2] != null ? r[2].toString() : null;
                        BigDecimal quantity = toBigDecimal(r[3]);
                        BigDecimal amountKrw = toBigDecimal(r[4]);
                        BigDecimal amountUsd = toBigDecimal(r[5]);
                        BigDecimal priceKrw = toBigDecimal(r[6]);
                        BigDecimal priceUsd = toBigDecimal(r[7]);
                        BigDecimal feeKrw = toBigDecimal(r[8]);
                        BigDecimal feeUsd = toBigDecimal(r[9]);
                        BigDecimal taxKrw = toBigDecimal(r[10]);
                        BigDecimal taxUsd = toBigDecimal(r[11]);

                        TradeHistory t = new TradeHistory();
                        t.setTradeDate(trDate);
                        t.setTradeType(tradeType);
                        t.setSymbolName(symbolName);
                        t.setQuantity(quantity);
                        t.setAmountKrw(amountKrw);
                        t.setAmountUsd(amountUsd);
                        t.setPriceKrw(priceKrw);
                        t.setPriceUsd(priceUsd);
                        t.setFeeKrw(feeKrw);
                        t.setFeeUsd(feeUsd);
                        t.setTaxKrw(taxKrw);
                        t.setTaxUsd(taxUsd);

                        return t;
                    }).toList());
        for(String symbol : symbols){
            List<TradeHistory> filteredList = tList.stream()
                    .filter(t -> symbol.equals(t.getSymbolName()))
                    .toList();
            ProfitResult p = computeForList_simple(symbol, filteredList);
            DashboardDetail toSave = new DashboardDetail();
            toSave.setUserId(userId);
            toSave.setSymbolName(symbol);
            toSave.setTradeType("매매손익");
            toSave.setProfitLossKrw(p.getProfitKrw());
            toSave.setProfitLossUsd(p.getProfitUsd());
            dashboardDetailService.saveOrUpdate(toSave);
        }
    }



    private ProfitResult computeForList_simple(String symbolName, List<TradeHistory> tList) {
        ProfitResult p = new ProfitResult(symbolName);

        BigDecimal totalBuyAmountKrw = BigDecimal.ZERO;  // 누적매입금
        BigDecimal totalBuyAmountUsd = BigDecimal.ZERO;
        BigDecimal totalBuyQty = BigDecimal.ZERO;        // 총구매수량
        BigDecimal currentQty = BigDecimal.ZERO;         // 현재보유수량

        for (TradeHistory t : tList) {
            BigDecimal qty = t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO;

            if ("구매".equals(t.getTradeType())) {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 구매 단가 계산 (수수료/세금 포함)
                BigDecimal buyPriceKrw = t.getPriceKrw() != null && t.getPriceKrw().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceKrw()
                        : (t.getAmountKrw() != null ? t.getAmountKrw().divide(qty, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                BigDecimal buyPriceUsd = t.getPriceUsd() != null && t.getPriceUsd().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceUsd()
                        : (t.getAmountUsd() != null ? t.getAmountUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                // 수수료/세금을 단가에 반영
                if (t.getFeeKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    buyPriceKrw = buyPriceKrw.add(t.getFeeKrw().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getTaxKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    buyPriceKrw = buyPriceKrw.add(t.getTaxKrw().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getFeeUsd() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    buyPriceUsd = buyPriceUsd.add(t.getFeeUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getTaxUsd() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    buyPriceUsd = buyPriceUsd.add(t.getTaxUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP));

                // 누적 데이터 업데이트
                totalBuyAmountKrw = totalBuyAmountKrw.add(buyPriceKrw.multiply(qty));
                totalBuyAmountUsd = totalBuyAmountUsd.add(buyPriceUsd.multiply(qty));
                totalBuyQty = totalBuyQty.add(qty);
                currentQty = currentQty.add(qty);

            } else if ("판매".equals(t.getTradeType())) {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 판매 단가 계산 (수수료/세금 차감)
                BigDecimal sellPriceKrw = t.getPriceKrw() != null && t.getPriceKrw().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceKrw()
                        : (t.getAmountKrw() != null ? t.getAmountKrw().divide(qty, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                BigDecimal sellPriceUsd = t.getPriceUsd() != null && t.getPriceUsd().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceUsd()
                        : (t.getAmountUsd() != null ? t.getAmountUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                if (t.getFeeKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceKrw = sellPriceKrw.subtract(t.getFeeKrw().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getTaxKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceKrw = sellPriceKrw.subtract(t.getTaxKrw().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getFeeUsd() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceUsd = sellPriceUsd.subtract(t.getFeeUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getTaxUsd() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceUsd = sellPriceUsd.subtract(t.getTaxUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP));

                // 평균매수단가 계산 (현재보유 > 0이면 현재보유, 아니면 총구매수량 기준)
                BigDecimal divisor = currentQty.compareTo(BigDecimal.ZERO) > 0 ? currentQty : totalBuyQty;
                if (divisor.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal avgBuyKrw = totalBuyAmountKrw.divide(divisor, 8, java.math.RoundingMode.HALF_UP);
                BigDecimal avgBuyUsd = totalBuyAmountUsd.divide(divisor, 8, java.math.RoundingMode.HALF_UP);

                // 실현손익 = (판매단가 - 평균매수단가) × 판매수량
                BigDecimal krwDiff = sellPriceKrw.subtract(avgBuyKrw).multiply(qty);
                BigDecimal usdDiff = sellPriceUsd.subtract(avgBuyUsd).multiply(qty);
                p.addProfit(krwDiff, usdDiff, qty);

                // 현재보유수량 차감
                currentQty = currentQty.subtract(qty);

                // 보유수량이 0이 되면 누적매입금도 0으로 리셋 (완전매도)
                if (currentQty.compareTo(BigDecimal.ZERO) <= 0) {
                    totalBuyAmountKrw = BigDecimal.ZERO;
                    totalBuyAmountUsd = BigDecimal.ZERO;
                    totalBuyQty = BigDecimal.ZERO;
                    currentQty = BigDecimal.ZERO;
                } else {
                    // 부분매도시 매입금도 비례적으로 차감
                    BigDecimal ratio = qty.divide(divisor, 8, java.math.RoundingMode.HALF_UP);
                    totalBuyAmountKrw = totalBuyAmountKrw.subtract(avgBuyKrw.multiply(qty));
                    totalBuyAmountUsd = totalBuyAmountUsd.subtract(avgBuyUsd.multiply(qty));
                }
            }
        }

        return p;
    }

    // 3) 심볼별로 순수량을 계산하는 Stream 방식 (동일 결과)
    public Map<String, BigDecimal> calcNetBySymbolStream(List<DashboardDetail> dList) {
        return dList.stream()
                .collect(Collectors.groupingBy(
                        DashboardDetail::getSymbolName,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                d -> {
                                    BigDecimal q = d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO;
                                    return "구매".equals(d.getTradeType()) ? q
                                            : "판매".equals(d.getTradeType()) ? q.negate()
                                            : BigDecimal.ZERO;
                                },
                                BigDecimal::add
                        )
                ));
    }

    public List<DashboardDetail> convertToDetailList(List<Object[]> raws) {

        return raws.stream().map(r -> {
            String tradeType = r[0] != null ? r[0].toString() : null;
            String symbolName = r[1] != null ? r[1].toString() : null;

            BigDecimal totalKrw = toBigDecimal(r[2]);
            BigDecimal totalUsd = toBigDecimal(r[3]);
            BigDecimal quantity = toBigDecimal(r[4]);
            BigDecimal avgKrw = toBigDecimal(r[5]);
            BigDecimal avgUsd = toBigDecimal(r[6]);

            DashboardDetail d = new DashboardDetail();
            d.setTradeType(tradeType);
            d.setSymbolName(symbolName);
            d.setTotalAmountKrw(totalKrw);
            d.setTotalAmountUsd(totalUsd);
            d.setQuantity(quantity);
            d.setAvgPriceKrw(avgKrw);
            d.setAvgPriceUsd(avgUsd);
            return d;
        }).toList();
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        try { return new BigDecimal(o.toString()); } catch (Exception ex) { return BigDecimal.ZERO; }
    }

}
