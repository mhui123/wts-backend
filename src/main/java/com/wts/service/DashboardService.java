package com.wts.service;

import com.wts.entity.DashboardDetail;
import com.wts.entity.DashboardInfo;
import com.wts.entity.PortfolioItem;
import com.wts.entity.TradeHistory;
import com.wts.model.*;
import com.wts.repository.DashboardDetailRepository;
import com.wts.repository.DashboardInfoRepository;
import com.wts.repository.PortfolioItemRepository;
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
    private final PortfolioItemRepository pRepo;
    @Autowired
    private DashboardDetailService dashboardDetailService;

    public DashboardService(TradeHistoryRepository thRepository, PortfolioItemRepository portfolioItemRepository) {
        this.thRepository = thRepository;
        this.pRepo = portfolioItemRepository;
    }

    public DashboardSummaryDto getDashboardData(Long userId){
        DashboardSummaryDto dto = new DashboardSummaryDto();
        List<PortfolioItem> pList = pRepo.findByUserId(userId);
        dto.setDetailList(pList);

        return dto;
    }

    public ProcessResult setDataToPortfolioItem(Long userId){
        try {
            List<PortfolioItemDto> fList = calculatePortfolio(userId);
            calProfitTo(userId, fList);
            return new ProcessResult(true, "포트폴리오 데이터 업데이트 완료");
        } catch (Exception e) {
            return new ProcessResult(false, "포트폴리오 엡데이트 실패 :" + e.getMessage(),"PORTFOLIO_UPDATE_ERROR" );
        }

    }

    private List<PortfolioItemDto> calculatePortfolio(Long userId){
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

        for (PortfolioItemDto p : pList) {
            PortfolioItemDto fp = finalPList.stream()
                    .filter(item -> item.getCompanyName().equals(p.getSymbolName()))
                    .findFirst()
                    .orElse(new PortfolioItemDto(userId, p.getSymbolName()));
            String tradeType = p.getTradeType();
            String companyName = p.getCompanyName() == null ? p.getSymbolName() : p.getCompanyName();
            if ("외화증권배당금입금".equals(tradeType)) {
                fp.setDividendKrw(p.getTotalAmountKrw());
                fp.setDividendUsd(p.getTotalAmountUsd());
            } else if("구매".equals(tradeType)) {
                fp.setAvgPriceUsd(p.getAvgPriceUsd());
                fp.setAvgPriceKrw(p.getAvgPriceKrw());
                fp.setTotalInvestmentUsd(p.getTotalAmountUsd());
                fp.setTotalInvestmentKrw(p.getTotalAmountKrw());
            } else if("판매".equals(tradeType)) {
                BigDecimal oldUsd = fp.getTotalInvestmentUsd() != null ? fp.getTotalInvestmentUsd() : BigDecimal.ZERO;
                BigDecimal oldKrw = fp.getTotalInvestmentKrw() != null ? fp.getTotalInvestmentKrw() : BigDecimal.ZERO;
                BigDecimal oldQty = fp.getQuantity() != null ? fp.getQuantity() : BigDecimal.ZERO;

                if(oldUsd.compareTo(BigDecimal.ZERO) > 0 && oldKrw.compareTo(BigDecimal.ZERO) > 0) {
                    // 판매한 금액을 차감 (현재 판매 거래의 금액)
                    BigDecimal sellAmountKrw = p.getTotalAmountKrw() != null ? p.getTotalAmountKrw() : BigDecimal.ZERO;
                    BigDecimal sellAmountUsd = p.getTotalAmountUsd() != null ? p.getTotalAmountUsd() : BigDecimal.ZERO;
                    BigDecimal sellQty = p.getQuantity() != null ? p.getQuantity() : BigDecimal.ZERO;

                    fp.setTotalInvestmentUsd(oldUsd.subtract(sellAmountUsd));
                    fp.setTotalInvestmentKrw(oldKrw.subtract(sellAmountKrw));
                    fp.setQuantity(oldQty.subtract(sellQty));

                    // 음수가 되지 않도록 보정
                    if(fp.getTotalInvestmentKrw().compareTo(BigDecimal.ZERO) < 0) {
                        fp.setTotalInvestmentKrw(BigDecimal.ZERO);
                    }
                    if(fp.getTotalInvestmentUsd().compareTo(BigDecimal.ZERO) < 0) {
                        fp.setTotalInvestmentUsd(BigDecimal.ZERO);
                    }
                }
            }
//            else if("주식병합출고".equals(tradeType)) {
//                BigDecimal oldQty = fp.getQuantity() != null ? fp.getQuantity() : BigDecimal.ZERO;
//                BigDecimal mergeOutQty = p.getQuantity() != null ? p.getQuantity() : BigDecimal.ZERO;
//                fp.setQuantity(oldQty.subtract(mergeOutQty));
//            } else if("주식병합입고".equals(tradeType)) {
//                BigDecimal oldQty = fp.getQuantity() != null ? fp.getQuantity() : BigDecimal.ZERO;
//                BigDecimal mergeInQty = p.getQuantity() != null ? p.getQuantity() : BigDecimal.ZERO;
//                fp.setQuantity(oldQty.add(mergeInQty));
//            }

            BigDecimal netQuantity = netBySymbol.getOrDefault(companyName, BigDecimal.ZERO);
            if (netQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                fp.setTotalInvestmentKrw(BigDecimal.ZERO);
                fp.setTotalInvestmentUsd(BigDecimal.ZERO);
            }

            fp.setQuantity(netQuantity);
            fp.setUserId(userId);
        }

        return finalPList;
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

            PortfolioItemDto d = new PortfolioItemDto();
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
