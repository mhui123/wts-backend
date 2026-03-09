package com.wts.summary.service;

import com.wts.api.dto.ProcessResult;
import com.wts.api.dto.StockDistributionDto;
import com.wts.api.dto.StockInfo;
import com.wts.api.dto.StockPriceResponseDto;
import com.wts.api.service.PythonServerService;
import com.wts.model.TradeSearchCondition;
import com.wts.summary.domain.Inoutcom;
import com.wts.summary.domain.portfolio.Portfolio;
import com.wts.summary.dto.*;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.enums.Currency;
import com.wts.summary.enums.FlowType;
import com.wts.summary.enums.YesNo;
import com.wts.summary.jpa.entity.*;
import com.wts.summary.jpa.mapper.PortfolioMapper;
import com.wts.summary.jpa.repository.JpaPortfolioItemRepository;
import com.wts.summary.jpa.repository.PendingFetchDividendRepository;
import com.wts.summary.jpa.repository.StockDistributionRepository;
import com.wts.summary.jpa.repository.SymbolTickerRepository;
import com.wts.summary.mapper.DashboardAssembler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DashboardService {

    private final JpaPortfolioItemRepository pRepo;
    private final SymbolTickerRepository sRepo;
    private final PythonServerService pService;
    private final StockDistributionRepository disRepo;
    private final PendingFetchDividendRepository pendingRepo;
    private final TradeHistoryService thService;
    private final PortfolioMapper pMapper;
    private final DashboardAssembler dashAssembler;

    public DashboardSummaryDto getDashboardData(Long userId){

        List<PortfolioItemEntity> pList = pRepo.findByUserId(userId);
        Portfolio portfolio = pMapper.toDomain(pList);
        DashboardSummaryDto dto = dashAssembler.toDashboardSummaryDto(portfolio);

        List<PortfolioItemDto> pdtoList = pList.stream().map(this::toDto).toList();
        dto.setDetailList(pdtoList);

        return dto;
    }

    //토스냐, 키움이냐, 국내주식이냐 미국주식이냐에 따라 거래타입이 다름..
    public StockDetailDto callStockDetailInfo(Long userId, String ticker, Currency currency, BrokerType brokerType){
        StockDetailDto dto = new StockDetailDto(ticker);
        String symbolName = sRepo.findByTicker(ticker)
                .map(SymbolTicker::getSymbolName)
                .orElse(ticker);
        currency = currency != null ? currency : Currency.USD; // 기본값 설정
        brokerType = brokerType != null ? brokerType : BrokerType.TOSS; // 기본값 설정

        List<String> dividendTypes = brokerType.typeToString(FlowType.DIVIDEND, currency);

        TradeSearchCondition condition = TradeSearchCondition.builder()
                .userId(userId)
                .symbolName(symbolName)
                .tradeTypes(dividendTypes)
                .brokerType(brokerType)
                .build();

        List<TradeHistory> rawTrList = thService.getHistoryWithinConditions(condition);

        List<DividendDetailDto> receivedInfo = rawTrList.stream()
                .map(DashboardAssembler::toDividendDetailDto)
                .toList();

        dto.setReceivedInfo(receivedInfo);

        List<StockDistribution> declaredInfo = disRepo.findByTicker(ticker);
        if(declaredInfo.isEmpty()){
            Optional<PendingFetchDividend> pendingOpt = pendingRepo.findByTicker(ticker);
            if(pendingOpt.isEmpty()){
                // 파이썬에서 새로 호출요청? -> 배당데이터 수요 요청. (대기 테이블에 적재 후 스케줄러가 처리하는 방식)
                PendingFetchDividend e = PendingFetchDividend.builder()
                        .ticker(ticker)
                        .status(YesNo.N)
                        .build();
                pendingRepo.save(e);
            }
        }
        List<StockDistributionDto> declaredDto = declaredInfo.stream()
                .map(DashboardAssembler::toStockDistributionDto)
                .toList();

        dto.setDeclaredInfo(declaredDto);

        return dto;
    }

    public ProcessResult getOcilatorInfo(String ticker, String period, String interval) {

        Map<String, Object> params = Map.of(
                "ticker", ticker,
                "period", period,
                "interval", interval
        );
        return pService.executeGetTask("/wpy/getOcilatorData", params);
    }


    @Transactional
    public ProcessResult updateClosePriceInfo(Long userId){
        List<PortfolioItemEntity> pList = pRepo.findByUserId(userId);
        List<String> tickers = pList.stream()
                .map(PortfolioItemEntity::getSymbol)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String symbols = String.join(",", tickers);
        StockPriceResponseDto dto = pService.getStockPrice(symbols);
        Map<String, StockInfo> stocks = dto.getStocks();

        for(String key : stocks.keySet()){
            StockInfo stockInfo = stocks.get(key);
            for(PortfolioItemEntity p : pList){
                if(key.equals(p.getSymbol())){
                    p.setCurrentPrice(stockInfo.getPrice());
                }
            }
        }

        return ProcessResult.builder()
                .success(true)
                .message("종가정보 업데이트 완료. " + userId)
                .build();
    }

    public ProcessResult getInOutcomeInfo(Long userId){
        try {
            // 계좌종류 조회한 후에 해당하는 입출금데이터 조회하도록 변경
            /* todo : 현재는 일단 토스 증권의 입출금 데이터만 조회하도록 구현된 상태이며
                    키움증권까지 포괄해서 조회할 수 있도록 로직 개선 필요 + 원화계좌 집계도 필요.
            *   */
            InoutcomDto dto = getTossInoutcomeDto(userId, Currency.USD);

            return ProcessResult.builder()
                    .success(true)
                    .message("입출금 금액정보 조회 완료.")
                    .data(dto)
                    .build();
        } catch (Exception e) {
            return ProcessResult.builder()
                    .success(false)
                    .message("입출금 금액정보 조회 실패: " + e.getMessage())
                    .build();
        }

    }

    private InoutcomDto getKiwoomInoutcomeDto(Long userId, Currency currency) {
        InoutcomDto dto = new InoutcomDto();
        return dto;
    }

    private InoutcomDto getTossInoutcomeDto(Long userId, Currency currency){
        InoutcomDto dto = new InoutcomDto();

        TradeSearchCondition condition = TradeSearchCondition.builder()
                .userId(userId)
                .currency(currency)
                .orderBy(TradeSearchCondition.OrderBy.TR_HIST_ID)
                .sortDirection(TradeSearchCondition.SortDirection.ASC)
                .build();

        List<TradeHistory> histories = thService.getHistoryWithinConditions(condition);
        Inoutcom summary = Inoutcom.summarize(histories, currency);

        dto.setIncomeSumKrw(summary.getIncomeSumKrw());
        dto.setOutcomeSumKrw(summary.getOutcomeSumKrw());
        dto.setDivSumKrw(summary.getDivSumKrw());
        dto.setOtherSumKrw(summary.getOtherSumKrw());
        dto.setIncomeSumUsd(summary.getIncomeSumUsd());
        dto.setOutcomeSumUsd(summary.getOutcomeSumUsd());
        dto.setDivSumUsd(summary.getDivSumUsd());
        dto.setOtherSumUsd(summary.getOtherSumUsd());

        return dto;
    }



    public PortfolioItemDto toDto(PortfolioItemEntity p) {
        return PortfolioItemDto.builder().
                userId(p.getUserId()).
                companyName(p.getCompanyName()).
                symbol(p.getSymbol()).
                quantity(p.getQuantity()).
                currentPrice(p.getCurrentPrice()).
                totalSell(p.getTotalSell()).
                totalBuy(p.getTotalBuy()).
                buyQty(p.getBuyQty()).
                sellQty(p.getSellQty()).
                avgSellPrice(p.getAvgSellPrice()).
                avgBuyPrice(p.getAvgBuyPrice()).
                profit(p.getProfit()).
                dividend(p.getDividend()).
                tax(p.getTax()).
                fee(p.getFee()).
                currency(p.getCurrency()).
                holdingAmount(p.getHoldingAmount()).
                holdingPrice(p.getHoldingPrice()).
                brokerType(p.getBrokerType()).
                build();
    }


}
