package com.wts.service;

import com.wts.entity.*;
import com.wts.model.*;
import com.wts.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = false)
public class DashboardService {

    @PersistenceContext
    private EntityManager em;

    private final TradeHistoryRepository thRepository;
    private final PortfolioItemRepository pRepo;
    private final SymbolTickerRepository sRepo;
    private final PythonServerService pService;
    private final StockDistributionRepository disRepo;

    public DashboardService(TradeHistoryRepository thRepository, PortfolioItemRepository portfolioItemRepository
            , SymbolTickerRepository sRepo, PythonServerService pService, StockDistributionRepository disRepo) {
        this.thRepository = thRepository;
        this.pRepo = portfolioItemRepository;
        this.sRepo = sRepo;
        this.pService = pService;
        this.disRepo = disRepo;
    }

    public DashboardSummaryDto getDashboardData(Long userId){
        DashboardSummaryDto dto = new DashboardSummaryDto();
        List<PortfolioItem> pList = pRepo.findByUserId(userId);
        List<PortfolioItemDto> pdtoList = pList.stream().map(PortfolioItem::toDto).toList();
        dto.setDetailList(pdtoList);

        return dto;
    }

    public ProcessResult setDataToPortfolioItem(Long userId){
        try {
            log.info("Syncronizing portfolio data for userId: {}", userId);
//            List<PortfolioItemDto> fList = calculatePortfolio(userId);
            List<PortfolioItemDto> fList = calculatePortfolio_renew(userId);
            calProfitTo(userId, fList);
            return new ProcessResult(true, "포트폴리오 데이터 업데이트 완료");
        } catch (Exception e) {
            return new ProcessResult(false, "포트폴리오 업데이트 실패 :" + e.getMessage(),"PORTFOLIO_UPDATE_ERROR" );
        }

    }

    public void calProfitTo(Long userId, List<PortfolioItemDto> pList){
        List<Object[]> rawList = thRepository.getProfitList(userId, List.of("구매", "판매"));
        List<PortfolioItemDto> tList = new ArrayList<>(rawList.stream().map(r -> {
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
            String isin = r[12] != null ? r[12].toString() : null;

            PortfolioItemDto t = new PortfolioItemDto();
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
            t.setIsin(isin);

            return t;
        }).toList());
        List<String> symbols = tList.stream()
                .map(PortfolioItemDto::getSymbolName)
                .distinct()
                .toList();
        PortfolioItem e = new PortfolioItem();
        for(String companyName : symbols){
            List<PortfolioItemDto> filteredList = tList.stream()
                    .filter(t -> companyName.equals(t.getSymbolName()))
                    .toList();
            ProfitResult p = computeForList_simple_renew(companyName, filteredList);

            PortfolioItemDto fdto = pList.stream()
                    .filter(item -> item.getCompanyName().equals(companyName))
                    .findFirst()
                    .orElse(null);
            if(fdto != null){
                //
                fdto.setProfitKrw(p.getProfitKrw());
                fdto.setProfitUsd(p.getProfitUsd());

                // 기존 엔티티 조회 후 업데이트 (PK 조건으로 findBy 메서드 필요)
                Optional<PortfolioItem> existing = pRepo.findByUserIdAndCompanyName(userId, companyName);
                if(existing.isPresent()) {
                    // 기존 엔티티 업데이트
                    PortfolioItem existingEntity = existing.get();
                    existingEntity.setProfitKrw(p.getProfitKrw());
                    existingEntity.setProfitUsd(p.getProfitUsd());
                    existingEntity.setAvgPriceKrw(fdto.getAvgPriceKrw());
                    existingEntity.setAvgPriceUsd(fdto.getAvgPriceUsd());
                    existingEntity.setQuantity(fdto.getQuantity());
                    existingEntity.setTotalInvestmentKrw(fdto.getTotalInvestmentKrw());
                    existingEntity.setTotalInvestmentUsd(fdto.getTotalInvestmentUsd());
                    existingEntity.setDividendKrw(fdto.getDividendKrw());
                    existingEntity.setDividendUsd(fdto.getDividendUsd());
                    existingEntity.setSymbol(fdto.getSymbol());
                    existingEntity.setTotalSellUsd(fdto.getTotalSellUsd());
                    existingEntity.setTotalSellKrw(fdto.getTotalSellKrw());

                    existingEntity.setSellQty(fdto.getSellQty());
                    existingEntity.setAvgSellPriceKrw(fdto.getAvgSellPriceKrw());
                    existingEntity.setAvgSellPriceUsd(fdto.getAvgSellPriceUsd());
                    existingEntity.setAvgBuyPriceKrw(fdto.getAvgBuyPriceKrw());
                    existingEntity.setAvgBuyPriceUsd(fdto.getAvgBuyPriceUsd());
                    existingEntity.setBuyQty(fdto.getBuyQty());
                    existingEntity.setTotalBuyKrw(fdto.getTotalBuyKrw());
                    existingEntity.setTotalBuyUsd(fdto.getTotalBuyUsd());

                    existingEntity.setFeeKrw(fdto.getFeeKrw());
                    existingEntity.setFeeUsd(fdto.getFeeUsd());
                    existingEntity.setTaxKrw(fdto.getTaxKrw());
                    existingEntity.setTaxUsd(fdto.getTaxUsd());

                    // 다른 필드들도 필요시 업데이트
                    pRepo.save(existingEntity); // UPDATE 실행
                } else {
                    // 새로 생성
                    pRepo.save(e.fromDto(fdto));
                }
            }

        }
    }
    /**
     * 단순 매매손익 계산
     * @param symbolName
     * @param tList
     * @return ProfitResult
     */
    private ProfitResult computeForList_simple_renew(String symbolName, List<PortfolioItemDto> tList) {
        ProfitResult p = new ProfitResult(symbolName);

        BigDecimal totalBuyAmountKrw = BigDecimal.ZERO;  // 누적매입금
        BigDecimal totalBuyAmountUsd = BigDecimal.ZERO;
        BigDecimal totalBuyQty = BigDecimal.ZERO;        // 총구매수량
        BigDecimal currentQty = BigDecimal.ZERO;         // 현재보유수량

        for (PortfolioItemDto t : tList) {
            BigDecimal qty = t.getQuantity() != null ? t.getQuantity() : BigDecimal.ZERO;

            if ("구매".equals(t.getTradeType())) {
                if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 구매 단가 계산 (수수료/세금 포함)
                BigDecimal buyPriceKrw = t.getPriceKrw() != null && t.getPriceKrw().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceKrw()
                        : (t.getAmountKrw() != null ? t.getAmountKrw().divide(qty, 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                BigDecimal buyPriceUsd = t.getPriceUsd() != null && t.getPriceUsd().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceUsd()
                        : (t.getAmountUsd() != null ? t.getAmountUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                // 수수료/세금을 단가에 반영
                if (t.getFeeKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    buyPriceKrw = buyPriceKrw.add(t.getFeeKrw().divide(qty, 0, java.math.RoundingMode.HALF_UP));
                if (t.getTaxKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    buyPriceKrw = buyPriceKrw.add(t.getTaxKrw().divide(qty, 0, java.math.RoundingMode.HALF_UP));
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
                // 판매단가 계산 (KRW는 정수로)
                BigDecimal sellPriceKrw = t.getPriceKrw() != null && t.getPriceKrw().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceKrw()
                        : (t.getAmountKrw() != null ? t.getAmountKrw().divide(qty, 0, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                BigDecimal sellPriceUsd = t.getPriceUsd() != null && t.getPriceUsd().compareTo(BigDecimal.ZERO) > 0
                        ? t.getPriceUsd()
                        : (t.getAmountUsd() != null ? t.getAmountUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO);

                if (t.getFeeKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceKrw = sellPriceKrw.subtract(t.getFeeKrw().divide(qty, 0, java.math.RoundingMode.HALF_UP));
                if (t.getTaxKrw() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceKrw = sellPriceKrw.subtract(t.getTaxKrw().divide(qty, 0, java.math.RoundingMode.HALF_UP));
                if (t.getFeeUsd() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceUsd = sellPriceUsd.subtract(t.getFeeUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP));
                if (t.getTaxUsd() != null && qty.compareTo(BigDecimal.ZERO) > 0)
                    sellPriceUsd = sellPriceUsd.subtract(t.getTaxUsd().divide(qty, 8, java.math.RoundingMode.HALF_UP));

                // 평균매수단가 계산 (현재보유 > 0이면 현재보유, 아니면 총구매수량 기준)
                BigDecimal divisor = currentQty.compareTo(BigDecimal.ZERO) > 0 ? currentQty : totalBuyQty;
                if (divisor.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal avgBuyKrw = totalBuyAmountKrw.divide(divisor, 0, java.math.RoundingMode.HALF_UP);
                BigDecimal avgBuyUsd = totalBuyAmountUsd.divide(divisor, 8, java.math.RoundingMode.HALF_UP);

                t.setAvgPriceUsd(avgBuyUsd);
                t.setAvgPriceKrw(avgBuyKrw);

                // 실현손익 = (판매단가 - 평균매수단가) × 판매수량
                BigDecimal krwDiff = sellPriceKrw.subtract(avgBuyKrw).multiply(qty);
                krwDiff = krwDiff.setScale(0, java.math.RoundingMode.HALF_UP);
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
//                    BigDecimal ratio = qty.divide(divisor, 8, java.math.RoundingMode.HALF_UP);
                    totalBuyAmountKrw = totalBuyAmountKrw.subtract(avgBuyKrw.multiply(qty));
                    totalBuyAmountUsd = totalBuyAmountUsd.subtract(avgBuyUsd.multiply(qty));
                }
            }
        }

        return p;
    }

    public Map<String, BigDecimal> calcNetBySymbolStream_renew(List<PortfolioItemDto> dList) {
        Map<String, BigDecimal> raw = dList.stream()
                .collect(Collectors.groupingBy(
                        PortfolioItemDto::getSymbolName,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                d -> {
                                    BigDecimal q = d.getQuantity() != null ? d.getQuantity() : BigDecimal.ZERO;
                                    return "구매".equals(d.getTradeType()) ? q
                                            : "판매".equals(d.getTradeType()) ? q.negate()
                                            : "주식병합출고".equals(d.getTradeType()) ? q.negate()
                                            : "주식병합입고".equals(d.getTradeType()) ? q
                                            : BigDecimal.ZERO;
                                },
                                BigDecimal::add
                        )
                ));
        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : e.getValue()
                ));
    }

    public List<PortfolioItemDto> convertToPList(List<Object[]> raws) {

        return raws.stream().map(r -> {
            String tradeType = r[0] != null ? r[0].toString() : null;
            String symbolName = r[1] != null ? r[1].toString() : null;

            BigDecimal totalKrw = toBigDecimal(r[2]);
            BigDecimal totalUsd = toBigDecimal(r[3]);
            BigDecimal quantity = toBigDecimal(r[4]);
            BigDecimal avgKrw = toBigDecimal(r[5]);
            BigDecimal avgUsd = toBigDecimal(r[6]);
            String isin = r[7] != null ? r[7].toString() : null;
            BigDecimal feeKrw = toBigDecimal(r[8]);
            BigDecimal feeUsd = toBigDecimal(r[9]);
            BigDecimal taxKrw = toBigDecimal(r[10]);
            BigDecimal taxUsd = toBigDecimal(r[11]);

            PortfolioItemDto d = new PortfolioItemDto();
            d.setTradeType(tradeType);
            d.setSymbolName(symbolName);
            d.setTotalAmountKrw(totalKrw);
            d.setTotalAmountUsd(totalUsd);
            d.setQuantity(quantity);
            d.setAvgPriceKrw(avgKrw);
            d.setAvgPriceUsd(avgUsd);
            d.setIsin(isin);

            d.setFeeUsd(feeUsd);
            d.setFeeKrw(feeKrw);
            d.setTaxUsd(taxUsd);
            d.setTaxKrw(taxKrw);

            return d;
        }).toList();
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        try { return new BigDecimal(o.toString()); } catch (Exception ex) { return BigDecimal.ZERO; }
    }

    private List<PortfolioItemDto> calculatePortfolio_renew(Long userId){
        //trade_history로부터 데이터를 얻어와서 계산.
        List<String> types = List.of("외화증권배당금입금", "구매", "판매", "주식병합출고", "주식병합입고");
        List<Object[]> rawList = thRepository.getGroupedTrList(userId, types);
        List<PortfolioItemDto> pList = new ArrayList<>(convertToPList(rawList));

        Map<String, BigDecimal> netBySymbol = calcNetBySymbolStream_renew(pList);
        List<PortfolioItemDto> finalPList = new ArrayList<>();

        for(String key : netBySymbol.keySet()){
            PortfolioItemDto p = new PortfolioItemDto(userId, key);
            finalPList.add(p);
        }

        // 종목별로 시간순 거래내역을 가져와서 처리 (엑셀 방식 적용)
        for(String companyName : netBySymbol.keySet()) {
            // 시간순 거래내역 조회 (기존 그룹화된 데이터가 아닌 개별 거래내역 필요)
            rawList = thRepository.getTradeHistoryBySymbolAndUserIdOrderByTradeDate(userId, companyName, types);

            // 시간순으로 정렬
            List<TradeHistoryDto> transactions = rawList.stream().map(r -> {
                LocalDate tDate = r[0] != null ? LocalDate.parse(r[0].toString()) : null;
                String tradeType = r[1] != null ? r[1].toString() : null;
                String symbolName = r[2] != null ? r[2].toString() : null;
                BigDecimal quantity = toBigDecimal(r[3]);
                BigDecimal amtKrw = toBigDecimal(r[4]);
                BigDecimal amtUsd = toBigDecimal(r[5]);
                BigDecimal prcKrw = toBigDecimal(r[6]);
                BigDecimal prcUsd = toBigDecimal(r[7]);
                BigDecimal feeKrw = toBigDecimal(r[8]);
                BigDecimal feeUsd = toBigDecimal(r[9]);
                BigDecimal taxKrw = toBigDecimal(r[10]);
                BigDecimal taxUsd = toBigDecimal(r[11]);
                String isin = r[12] != null ? r[12].toString() : null;

                TradeHistoryDto d = new TradeHistoryDto();
                d.setTradeDate(tDate);
                d.setTradeType(tradeType);
                d.setSymbolName(symbolName);
                d.setQuantity(quantity);
                d.setAmountKrw(amtKrw);
                d.setAmountUsd(amtUsd);
                d.setPriceKrw(prcKrw);
                d.setPriceUsd(prcUsd);
                d.setFeeKrw(feeKrw);
                d.setFeeUsd(feeUsd);
                d.setTaxKrw(taxKrw);
                d.setTaxUsd(taxUsd);
                d.setIsin(isin);

                return d;
            }).toList();

            PortfolioItemDto fp = finalPList.stream()
                    .filter(item -> item.getCompanyName().equals(companyName))
                    .findFirst()
                    .orElse(new PortfolioItemDto(userId, companyName));

            // 심볼 정보 설정
            setSymbolInfo(fp, transactions.isEmpty() ? null : transactions.get(0).getIsin());

            // 엑셀 방식: 전량매도 기점 리셋을 고려한 계산
            PortfolioItemDto result = calculateInvestmentWithResets(transactions);

            // 결과 설정
            fp.setTotalBuyUsd(result.getTotalBuyUsd());
            fp.setTotalBuyKrw(result.getTotalBuyKrw());
            fp.setBuyQty(result.getBuyQty());
            fp.setTotalSellUsd(result.getTotalSellUsd());
            fp.setTotalSellKrw(result.getTotalSellKrw());
            fp.setSellQty(result.getSellQty());
            fp.setDividendUsd(result.getDividendUsd());
            fp.setDividendKrw(result.getDividendKrw());
            fp.setFeeUsd(result.getFeeUsd());
            fp.setFeeKrw(result.getFeeKrw());
            fp.setTaxUsd(result.getTaxUsd());
            fp.setTaxKrw(result.getTaxKrw());

            // 평균 단가 설정
            fp.setAvgBuyPriceUsd(result.getAvgBuyPriceUsd());
            fp.setAvgBuyPriceKrw(result.getAvgBuyPriceKrw());
            fp.setAvgSellPriceUsd(result.getAvgSellPriceUsd());
            fp.setAvgSellPriceKrw(result.getAvgSellPriceKrw());

            // 투자원금 설정 (엑셀 공식 결과)
            BigDecimal netQuantity = netBySymbol.getOrDefault(companyName, BigDecimal.ZERO);
            if (netQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                fp.setTotalInvestmentKrw(BigDecimal.ZERO);
                fp.setTotalInvestmentUsd(BigDecimal.ZERO);
            } else {
                // 엑셀 방식: 전량매도 이후 구간의 투자원금 계산
                fp.setTotalInvestmentKrw(result.getTotalInvestmentKrw().compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result.getTotalInvestmentKrw());
                fp.setTotalInvestmentUsd(result.getTotalInvestmentUsd().compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result.getTotalInvestmentUsd());
            }

            fp.setQuantity(netQuantity);
            fp.setUserId(userId);

            log.debug("티커:{} 투자원금usd:{} 리셋기점 총구매금액 - (판매직전 평균매수단가 × 리셋후 총판매수량) : {} - ({} * {})",
                    fp.getSymbol(),
                    fp.getTotalInvestmentUsd(),
                    result.getResetPointBuyUsd(),
                    result.getPreSellAvgPriceUsd(),
                    result.getFinalSellQty()
            );
        }

        return finalPList;
    }

    /**
     * 전량매도 기점 리셋을 고려한 투자원금 계산 (엑셀 로직 구현)
     */
    private PortfolioItemDto calculateInvestmentWithResets(List<TradeHistoryDto> transactions) {
        PortfolioItemDto result = new PortfolioItemDto();

        // 현재 구간 추적 변수
        BigDecimal currentQty = BigDecimal.ZERO;
        BigDecimal currentBuyAmountUsd = BigDecimal.ZERO;
        BigDecimal currentBuyAmountKrw = BigDecimal.ZERO;
        BigDecimal currentBuyQty = BigDecimal.ZERO;
        BigDecimal currentSellQty = BigDecimal.ZERO;

        // 최종 구간(현재 보유분) 추적 변수
        BigDecimal finalBuyAmountUsd = BigDecimal.ZERO;
        BigDecimal finalBuyAmountKrw = BigDecimal.ZERO;
        BigDecimal finalBuyQty = BigDecimal.ZERO;
        BigDecimal finalSellQty = BigDecimal.ZERO;
        BigDecimal preSellAvgPriceUsd = BigDecimal.ZERO;
        BigDecimal preSellAvgPriceKrw = BigDecimal.ZERO;

        // 전체 누적 변수 (통계용)
        BigDecimal totalSellAmountUsd = BigDecimal.ZERO;
        BigDecimal totalSellAmountKrw = BigDecimal.ZERO;
        BigDecimal totalDividendUsd = BigDecimal.ZERO;
        BigDecimal totalDividendKrw = BigDecimal.ZERO;
        BigDecimal totalFeeUsd = BigDecimal.ZERO;
        BigDecimal totalFeeKrw = BigDecimal.ZERO;
        BigDecimal totalTaxUsd = BigDecimal.ZERO;
        BigDecimal totalTaxKrw = BigDecimal.ZERO;

        boolean inFinalSegment = false; // 현재 최종 구간(전량매도 후 마지막 구간)인지 여부

        for (TradeHistoryDto tx : transactions) {
            String tradeType = tx.getTradeType();
            BigDecimal qty = tx.getQuantity() != null ? tx.getQuantity() : BigDecimal.ZERO;
            BigDecimal amountUsd = tx.getAmountUsd() != null ? tx.getAmountUsd() : BigDecimal.ZERO;
            BigDecimal amountKrw = tx.getAmountKrw() != null ? tx.getAmountKrw() : BigDecimal.ZERO;

            if ("외화증권배당금입금".equals(tradeType)) {
                totalDividendUsd = totalDividendUsd.add(amountUsd);
                totalDividendKrw = totalDividendKrw.add(amountKrw);

            } else if ("구매".equals(tradeType)) {
                currentQty = currentQty.add(qty);
                currentBuyAmountUsd = currentBuyAmountUsd.add(amountUsd);
                currentBuyAmountKrw = currentBuyAmountKrw.add(amountKrw);
                currentBuyQty = currentBuyQty.add(qty);

                // 수수료/세금 누적
                totalFeeUsd = totalFeeUsd.add(tx.getFeeUsd() != null ? tx.getFeeUsd() : BigDecimal.ZERO);
                totalFeeKrw = totalFeeKrw.add(tx.getFeeKrw() != null ? tx.getFeeKrw() : BigDecimal.ZERO);
                totalTaxUsd = totalTaxUsd.add(tx.getTaxUsd() != null ? tx.getTaxUsd() : BigDecimal.ZERO);
                totalTaxKrw = totalTaxKrw.add(tx.getTaxKrw() != null ? tx.getTaxKrw() : BigDecimal.ZERO);

            } else if ("판매".equals(tradeType)) {
                // 판매 직전 평균단가 계산 및 저장
                if (currentBuyQty.compareTo(BigDecimal.ZERO) > 0) {
                    preSellAvgPriceUsd = currentBuyAmountUsd.divide(currentBuyQty, 8, java.math.RoundingMode.HALF_UP);
                    preSellAvgPriceKrw = currentBuyAmountKrw.divide(currentBuyQty, 0, java.math.RoundingMode.HALF_UP);
                }

                currentQty = currentQty.subtract(qty);
                totalSellAmountUsd = totalSellAmountUsd.add(amountUsd);
                totalSellAmountKrw = totalSellAmountKrw.add(amountKrw);

                // 수수료/세금 누적
                totalFeeUsd = totalFeeUsd.add(tx.getFeeUsd() != null ? tx.getFeeUsd() : BigDecimal.ZERO);
                totalFeeKrw = totalFeeKrw.add(tx.getFeeKrw() != null ? tx.getFeeKrw() : BigDecimal.ZERO);
                totalTaxUsd = totalTaxUsd.add(tx.getTaxUsd() != null ? tx.getTaxUsd() : BigDecimal.ZERO);
                totalTaxKrw = totalTaxKrw.add(tx.getTaxKrw() != null ? tx.getTaxKrw() : BigDecimal.ZERO);

                // 전량매도 감지
                if (currentQty.compareTo(BigDecimal.ZERO) <= 0) {
                    log.debug("[{}], {} 전량매도 발생", tx.getTradeDate(), tx.getSymbolName());
                    //전량매도 후 다음 거래가 없을 경우 데이터 보존을 위함
                    finalBuyAmountUsd = currentBuyAmountUsd;
                    finalBuyAmountKrw = currentBuyAmountKrw;
                    finalBuyQty = currentBuyQty;
                    // 리셋: 다음 구매부터 새로운 구간 시작
                    currentBuyAmountUsd = BigDecimal.ZERO;
                    currentBuyAmountKrw = BigDecimal.ZERO;
                    currentBuyQty = BigDecimal.ZERO;
                    currentQty = BigDecimal.ZERO;

                    // 기존 최종 구간 데이터 초기화 (새로운 구간 시작)
                    finalSellQty = BigDecimal.ZERO;
                    inFinalSegment = true;
                } else {
                    // 부분매도: 최종 구간에서 판매수량 누적
                    log.debug("[{}], {} {}개 부분매도 발생", tx.getTradeDate(), tx.getSymbolName(), tx.getQuantity());
                    if (inFinalSegment) {
                        finalSellQty = finalSellQty.add(qty);
                    } else {
                        currentSellQty = currentSellQty.add(qty);
                    }
                }
            }

            // 현재 보유수량이 있고 전량매도 이후라면 최종 구간 데이터 업데이트
            if (currentQty.compareTo(BigDecimal.ZERO) > 0 && inFinalSegment) {
                log.debug("[{}], {} 최종 구간 데이터 업데이트 {} -> {} , {} -> {}", tx.getTradeDate(), tx.getSymbolName(),  finalBuyAmountUsd, currentBuyAmountUsd,  finalBuyQty, currentBuyQty);
                finalBuyAmountUsd = currentBuyAmountUsd;
                finalBuyAmountKrw = currentBuyAmountKrw;
                finalBuyQty = currentBuyQty;
            }
        }

        // 최종 구간이 없다면 전체 구간을 최종 구간으로 사용
        if (!inFinalSegment) {
            log.debug("최종 구간이 없다");
            finalBuyAmountUsd = currentBuyAmountUsd;
            finalBuyAmountKrw = currentBuyAmountKrw;
            finalBuyQty = currentBuyQty;
            finalSellQty = currentSellQty;
        }

        // 엑셀 공식 적용: 최종구간 총구매금액 - (판매직전 평균매수단가 × 최종구간 판매수량)
        BigDecimal investmentUsd = finalBuyAmountUsd;
        BigDecimal investmentKrw = finalBuyAmountKrw;

        if (finalSellQty.compareTo(BigDecimal.ZERO) > 0 && preSellAvgPriceUsd.compareTo(BigDecimal.ZERO) > 0) {
            investmentUsd = finalBuyAmountUsd.subtract(preSellAvgPriceUsd.multiply(finalSellQty));
            investmentKrw = finalBuyAmountKrw.subtract(preSellAvgPriceKrw.multiply(finalSellQty));
        }

        // 결과 설정
        result.setTotalBuyUsd(finalBuyAmountUsd); // 현재 구간 구매금액
        result.setTotalBuyKrw(finalBuyAmountKrw);
        result.setBuyQty(finalBuyQty);
        result.setTotalSellUsd(totalSellAmountUsd);
        result.setTotalSellKrw(totalSellAmountKrw);
        result.setSellQty(finalSellQty); // 최종 구간 판매수량
        result.setDividendUsd(totalDividendUsd);
        result.setDividendKrw(totalDividendKrw);
        result.setFeeUsd(totalFeeUsd);
        result.setFeeKrw(totalFeeKrw);
        result.setTaxUsd(totalTaxUsd);
        result.setTaxKrw(totalTaxKrw);
        result.setTotalInvestmentUsd(investmentUsd);
        result.setTotalInvestmentKrw(investmentKrw);

        // 로그용 추가 정보
        result.setResetPointBuyUsd(finalBuyAmountUsd);
        result.setPreSellAvgPriceUsd(preSellAvgPriceUsd);
        result.setFinalSellQty(finalSellQty);
        return result;
    }

    // 심볼 정보 설정 메서드 분리
    private void setSymbolInfo(PortfolioItemDto fp, String isin) {
        if (isin == null) return;

        Optional<SymbolTicker> found = sRepo.findByIsin(isin);
        if (found.isPresent()) {
            SymbolTicker s = found.get();
            String symbol = s.getTicker();
            if(symbol != null && !symbol.isEmpty()) {
                fp.setSymbol(s.getTicker());
            } else {
                ProcessResult r = pService.getTicker(isin);
                String ticker = r.isSuccess() ? r.getMessage() : "";
                if(ticker != null && !ticker.isEmpty()) {
                    fp.setSymbol(ticker);
                    s.setTicker(ticker);
                    sRepo.save(s);
                }
            }
        } else {
            ProcessResult r = pService.getTicker(isin);
            String ticker = r.isSuccess() ? r.getMessage() : "";
            if(ticker != null && !ticker.isEmpty()) {
                fp.setSymbol(ticker);
                SymbolTicker newSymbolTicker = new SymbolTicker();
                newSymbolTicker.setIsin(isin);
                newSymbolTicker.setTicker(ticker);
                newSymbolTicker.setSymbolName(fp.getCompanyName());
                sRepo.save(newSymbolTicker);
            }
        }
    }

    public StockDetailDto callStockDetailInfo(Long userId, String ticker){
        StockDetailDto dto = new StockDetailDto(ticker);
        String symbolName = sRepo.findByTicker(ticker)
                .map(SymbolTicker::getSymbolName)
                .orElse(ticker);

        List<TradeHistory> rawTrList = thRepository.findByUserIdAndSymbolNameAndTradeType(userId, symbolName, "외화증권배당금입금");
        List<DividendDetailDto> receivedInfo = rawTrList.stream()
                .map(TradeHistory::toDividendDto)
                .toList();

        dto.setReceivedInfo(receivedInfo);

        List<StockDistribution> declaredInfo = disRepo.findByTicker(ticker);
        List<StockDistributionDto> declaredDto = declaredInfo.stream()
                .map(StockDistribution::toDto)
                .toList();

        dto.setDeclaredInfo(declaredDto);

        return dto;
    }

}
