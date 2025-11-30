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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        List<DashboardDetail> dList = convertToDetailList(rawList);
        List<DashboardDetail> mutable = new ArrayList<>(dList);

        Map<String, BigDecimal> netBySymbol = calcNetBySymbolStream(dList);

//        List<String> symbols = dList.stream()
//                .map(DashboardDetail::getSymbolName)
//                .filter(Objects::nonNull)
//                .distinct().toList();
        // 배당금입금 분리
        List<DashboardDetail> dividends = dList.stream()
                .filter(d -> "외화증권배당금입금".equals(d.getTradeType()))
                .toList();
        mutable.removeIf(d -> "외화증권배당금입금".equals(d.getTradeType()));
        dList = mutable;


        for (int i = 0; i < dList.size(); i++) {
            DashboardDetail d = dList.get(i);
            d.setUserId(userId);
            DashboardDetail saved = dashboardDetailService.saveOrUpdate(d); // saveOrUpdate는 REQUIRES_NEW로 실행되어야 함
            if (saved != null) {
                dList.set(i, saved); // DB에서 영속화된 엔티티(id 포함)로 교체
            }
        }

        for (int i = 0; i < dividends.size(); i++) {
            DashboardDetail d = dividends.get(i);
            BigDecimal divK = d.getTotalAmountKrw();
            BigDecimal divU = d.getTotalAmountUsd();
            d.setDividendKrw(divK);
            d.setDividendUsd(divU);
            d.setTotalAmountKrw(BigDecimal.ZERO);
            d.setTotalAmountUsd(BigDecimal.ZERO);

            String symbol = d.getSymbolName();
            BigDecimal netQuantity = netBySymbol.getOrDefault(symbol, BigDecimal.ZERO);
            d.setQuantity(netQuantity);
            d.setUserId(userId);

            // 저장 및 반환값으로 대체
            DashboardDetail saved = dashboardDetailService.saveOrUpdate(d);
            if (saved != null) {
                dividends.set(i, saved);
            }

            totalUsd = totalUsd.add(d.getDividendUsd() != null ? d.getDividendUsd() : BigDecimal.ZERO);
            totalKrw = totalKrw.add(d.getDividendKrw() != null ? d.getDividendKrw() : BigDecimal.ZERO);
        }

        info.setTotalDividendUsd(totalUsd);
        info.setTotalDividendKrw(totalKrw);

        return dList;
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


    /**
     * Dashboard summary: 총 투자금(USD 기준)을 반환합니다.
     * tradeType='환전외화입금'인 레코드의 amountUsd 합계를 계산합니다.
     * userId가 제공되면 해당 사용자의 합계만 반환합니다.
     */
//    public DashboardSummaryDto getDashboardSummary(Long userId) {
//        String type = "환전외화입금";
//        SummaryRecord r;
//        BigDecimal totalUsd;
//        BigDecimal totalKrw;
//        if (userId != null) {
//            r = dashboardInfoRepository.sumAmountByTradeTypeAndUserId(type, userId);
//        } else {
//            r = dashboardInfoRepository.sumAmountUsdByTradeType(type);
//        }
//        if (r == null) {
//            totalUsd = BigDecimal.ZERO;
//            totalKrw = BigDecimal.ZERO;
//        } else {
//            totalUsd = r.sumUsd() != null ? r.sumUsd() : BigDecimal.ZERO;
//            totalKrw = r.sumKrw() != null ? r.sumKrw() : BigDecimal.ZERO;
//        }
//
//        List<String> stockList = new ArrayList<>(); // 추후 필요시 구현
//        List<String> types = List.of("구매", "판매");
//        stockList = thRepository.findDistinctSymbolNameByUserIdAndTradeTypeIn(userId, types);
//
//        List<Optional<DashboardStockDto>> dtoList = new ArrayList<>();
//        for(String n : stockList) {
//            Optional<DashboardStockDto> dto = findLatestSymbolQuantity_new(userId, n,types);
//            dtoList.add(dto);
//        }
//        return DashboardSummaryDto.builder()
//                .totalInvestmentUsd(totalUsd)
//                .totalInvestmentKrw(totalKrw)
//                .stockList(dtoList)
//                .build();
//    }
//
    public List<DashboardDetail> convertToDetailList(List<Object[]> raws) {

        return raws.stream().map(r -> {
            String tradeType = r[0] != null ? r[0].toString() : null;
            String symbolName = r[1] != null ? r[1].toString() : null;

            BigDecimal totalKrw = toBigDecimal(r[2]);
            BigDecimal totalUsd = toBigDecimal(r[3]);
            BigDecimal quantity = toBigDecimal(r[4]);

            DashboardDetail d = new DashboardDetail();
            d.setTradeType(tradeType);
            d.setSymbolName(symbolName);
            d.setTotalAmountKrw(totalKrw);
            d.setTotalAmountUsd(totalUsd);
            d.setQuantity(quantity);
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
