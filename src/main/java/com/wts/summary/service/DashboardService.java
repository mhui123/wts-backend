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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DashboardService {

    @PersistenceContext
    private EntityManager em;

    private static final int CHUNK_SIZE = 500; // 청크당 처리 건수

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

    public List<String> sendPortfolioSymbolList(){
        List<PortfolioItemEntity> pList = pRepo.findAll();

        return pList.stream()
                .map(PortfolioItemEntity::getSymbol)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    // 배당 정보 수집이 필요한 종목 목록 조회 (대기 테이블에서 상태가 N인 항목의 ticker 목록)
    public List<String> sendPendingList(){
        List<PendingFetchDividend> pList = pendingRepo.findByStatus(YesNo.N);

        return pList.stream()
                .map(PendingFetchDividend::getTicker)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    public void updatePendigList(List<String> tickers){
        List<PendingFetchDividend> pList = pendingRepo.findByTickerIn(tickers);

        for(PendingFetchDividend p : pList){
            p.setStatus(YesNo.Y);
        }
    }


    /**
     * 대량 배당 정보 upsert 저장
     * - ticker 그룹 단위로 청크를 나눠 처리하여 메모리 및 트랜잭션 크기 제어
     * - 청크당 bulk 조회로 N+1 쿼리 방지
     * - 청크마다 flush + clear로 JPA 1차 캐시 비대화 방지
     */
    public ProcessResult saveStockDistribution(List<StockDistributionDto> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return ProcessResult.builder()
                    .success(false)
                    .message("저장할 배당 정보가 없습니다.")
                    .build();
        }

        // ticker별로 그룹핑 → 순서 유지(LinkedHashMap)
        Map<String, List<StockDistributionDto>> grouped = dtoList.stream()
                .filter(dto -> dto.getTicker() != null && dto.getDeclaredDate() != null)
                .collect(Collectors.groupingBy(StockDistributionDto::getTicker,
                        LinkedHashMap::new, Collectors.toList()));

        int skippedNullKey = (int) dtoList.stream()
                .filter(dto -> dto.getTicker() == null || dto.getDeclaredDate() == null)
                .count();

        // ticker 목록을 CHUNK_SIZE 단위로 분할하여 처리
        List<String> tickers = new ArrayList<>(grouped.keySet());
        Map<String, int[]> tickerStat = new LinkedHashMap<>();

        for (int i = 0; i < tickers.size(); i += CHUNK_SIZE) {
            List<String> chunkTickers = tickers.subList(i, Math.min(i + CHUNK_SIZE, tickers.size()));
            List<StockDistributionDto> chunkDtos = chunkTickers.stream()
                    .flatMap(t -> grouped.get(t).stream())
                    .toList();

            Map<String, int[]> chunkStat = saveChunk(chunkDtos, chunkTickers);
            tickerStat.putAll(chunkStat);

            log.debug("청크 처리 완료: {}/{} tickers", Math.min(i + CHUNK_SIZE, tickers.size()), tickers.size());
        }

        // 집계
        List<Map<String, Object>> statList = tickerStat.entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "ticker", e.getKey(),
                        "insert", e.getValue()[0],
                        "update", e.getValue()[1],
                        "skip",   e.getValue()[2]
                ))
                .toList();

        int totalInsert = tickerStat.values().stream().mapToInt(s -> s[0]).sum();
        int totalUpdate = tickerStat.values().stream().mapToInt(s -> s[1]).sum();
        int totalSkip   = tickerStat.values().stream().mapToInt(s -> s[2]).sum() + skippedNullKey;

        log.info("배당 정보 전체 저장 완료 - 종목수={}, insert={}, update={}, skip={}",
                tickerStat.size(), totalInsert, totalUpdate, totalSkip);

        return ProcessResult.builder()
                .success(true)
                .message("배당 정보 저장 완료. (신규: %d, 업데이트: %d, 건너뜀: %d)".formatted(totalInsert, totalUpdate, totalSkip))
                .data(statList)
                .build();
    }

    /**
     * 청크 단위 upsert 처리
     * - chunkTickers에 해당하는 기존 DB 레코드를 bulk 조회 (N+1 방지)
     * - 처리 후 flush + clear로 영속성 컨텍스트 정리
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Map<String, int[]> saveChunk(List<StockDistributionDto> chunkDtos, List<String> chunkTickers) {
        Map<String, int[]> stat = new LinkedHashMap<>();

        // ticker 목록 전체를 쿼리 1회로 조회 → (ticker + declaredDate) 복합 키로 맵 구성
        Map<String, StockDistribution> existingMap = disRepo.findByTickerIn(chunkTickers).stream()
                .collect(Collectors.toMap(
                        d -> d.getTicker() + "|" + d.getDeclaredDate(),
                        Function.identity()
                ));

        List<StockDistribution> toInsert = new ArrayList<>();

        for (StockDistributionDto dto : chunkDtos) {
            String key = dto.getTicker() + "|" + dto.getDeclaredDate();
            int[] tickerStat = stat.computeIfAbsent(dto.getTicker(), k -> new int[]{0, 0, 0});

            try {
                if (existingMap.containsKey(key)) {
                    // update: dirty checking 활용
                    StockDistribution entity = existingMap.get(key);
                    entity.setDistributionPerShare(dto.getDistributionPerShare());
                    entity.setRocPct(dto.getRocPct());
                    entity.setExDate(dto.getExDate());
                    entity.setRecordDate(dto.getRecordDate());
                    entity.setPayableDate(dto.getPayableDate());
                    tickerStat[1]++;
                } else {
                    // insert: 배치용으로 모아서 saveAll
                    toInsert.add(StockDistribution.builder()
                            .ticker(dto.getTicker())
                            .distributionPerShare(dto.getDistributionPerShare())
                            .rocPct(dto.getRocPct())
                            .declaredDate(dto.getDeclaredDate())
                            .exDate(dto.getExDate())
                            .recordDate(dto.getRecordDate())
                            .payableDate(dto.getPayableDate())
                            .build());
                    tickerStat[0]++;
                }
            } catch (Exception e) {
                log.error("배당 항목 처리 실패: ticker={}, declaredDate={}, error={}",
                        dto.getTicker(), dto.getDeclaredDate(), e.getMessage(), e);
                tickerStat[2]++;
            }
        }

        // insert 배치 처리
        if (!toInsert.isEmpty()) {
            disRepo.saveAll(toInsert);
        }

        // 1차 캐시 비대화 방지: 청크 처리 후 flush + clear
        em.flush();
        em.clear();

        return stat;
    }

}
