package com.wts.summary.service;

import com.wts.api.dto.ProcessResult;
import com.wts.auth.jpa.entity.User;
import com.wts.auth.jpa.repository.UserRepository;
import com.wts.model.TradeSearchCondition;
import com.wts.summary.adapter.TradeHistoryNdjsonAdapter;
import com.wts.summary.domain.PortfolioItemUpdater;
import com.wts.summary.domain.StockTradeAggregator;
import com.wts.summary.domain.service.TradeHistoryDomainService;
import com.wts.summary.dto.PortfolioItemDto;
import com.wts.summary.dto.TradeHistoryDto;
import com.wts.summary.dto.TradeHistoryJsonRecord;
import com.wts.summary.enums.BrokerType;
import com.wts.summary.jpa.entity.PortfolioItemEntity;
import com.wts.summary.jpa.entity.SymbolTicker;
import com.wts.summary.jpa.entity.TradeHistory;
import com.wts.summary.jpa.repository.JpaPortfolioItemRepository;
import com.wts.summary.jpa.repository.SymbolTickerRepository;
import com.wts.summary.jpa.repository.TradeHistoryRepository;
import com.wts.summary.jpa.repository.query.TradeHistorySpecification;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TradeHistoryService {
    private static final Logger log = LoggerFactory.getLogger(TradeHistoryService.class);

    private final TradeHistoryRepository repository;
    private final UserRepository uRepo;
    private final SymbolTickerRepository symbolTickerRepo;
    private final JpaPortfolioItemRepository portfolioItemRepo;
    private final TradeHistoryNdjsonAdapter ndjsonAdapter;
    private final TradeHistoryDomainService tradeHistoryDomainService;
    
    public List<TradeHistoryDto> getTrades(Long userId,
                                           LocalDate fromDate,
                                           LocalDate toDate,
                                           String tradeType,
                                           String symbolName,
                                           Integer page,
                                           Integer size) {

        TradeSearchCondition condition = TradeSearchCondition.builder()
                .userId(userId)
                .startDate(fromDate)
                .endDate(toDate)
                .tradeType(tradeType)
                .symbolName(symbolName)
                .page(page)
                .size(size)
                .build();

        List<TradeHistory> entities = getHistoryWithinConditions(condition);

        return entities.stream()
                .map(t -> TradeHistoryDto.builder()
                        .trHistId(t.getTrHistId())
                        .userId(t.getUser() != null ? t.getUser().getId() : null)
                        .tradeDate(t.getTradeDate())
                        .tradeType(t.getTradeType())
                        .symbolName(t.getSymbolName())
                        .fxRate(t.getFxRate())
                        .quantity(t.getQuantity())
                        .amountKrw(t.getAmountKrw())
                        .amountUsd(t.getAmountUsd())
                        .priceKrw(t.getPriceKrw())
                        .priceUsd(t.getPriceUsd())
                        .balanceQty(t.getBalanceQty())
                        .balanceKrw(t.getBalanceKrw())
                        .balanceUsd(t.getBalanceUsd())
                        .build())
                .collect(Collectors.toList());
    }

    /*
    * 거래내역을 바탕으로 각 종목별로 집계 및 저장
    * */
    public ProcessResult summarizeTradeHistoryAsEachStocks(Long userId, BrokerType brokerType) {
        try{
            List<String> stockTypes = StockTradeAggregator.stockTradeTypes(brokerType);

            TradeSearchCondition conditons = TradeSearchCondition.builder()
                    .userId(userId)
                    .tradeTypes(stockTypes)
                    .orderBy(TradeSearchCondition.OrderBy.TRADE_DATE)
                    .sortDirection(TradeSearchCondition.SortDirection.ASC)
                    .build();

            List<TradeHistory> stockTrades = getHistoryWithinConditions(conditons);

            List<PortfolioItemDto> plist = StockTradeAggregator.summarizeBySymbol(
                    userId,
                    brokerType,
                    stockTrades,
                    this::getTickerByIsin
            );

            if(processToPortfolioItem(plist)){
                return ProcessResult.builder()
                        .success(true)
                        .message("포트폴리오 요약 완료: " + plist.size() + "개 종목 처리")
                        .data(plist)
                        .processedCount(plist.size())
                        .build();
            } else {
                return ProcessResult.builder()
                        .success(false)
                        .message("포트폴리오 요약 처리 실패")
                        .build();
            }

        } catch (Exception e){
            return ProcessResult.builder()
                    .success(false)
                    .message("포트폴리오 요약 실패: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    private boolean processToPortfolioItem(List<PortfolioItemDto> pList){

        for(PortfolioItemDto p : pList){
            Long userId = p.getUserId();
            String companyName = p.getCompanyName();
            Optional<PortfolioItemEntity> existing = portfolioItemRepo.findByUserIdAndCompanyName(userId, companyName);
            if(existing.isPresent()) {
                // 기존 엔티티 업데이트
                PortfolioItemEntity existingEntity = existing.get();
                PortfolioItemUpdater.applyUpdates(existingEntity, p);
            } else {
                // 새로 생성
                portfolioItemRepo.save(PortfolioItemUpdater.toEntity(p));
            }
        }

        return true;
    }

    private String getTickerByIsin(String isin, String symbolName){
        Optional<SymbolTicker> opt = symbolTickerRepo.findByIsin(isin);

        //만약 테이블에 없으면 우선 symbolticker table에 ticker를 공란으로 추가한다.
        if(opt.isEmpty()){
            SymbolTicker symbolTicker = SymbolTicker.builder()
                    .isin(isin)
                    .symbolName(symbolName)
                    .build();
            symbolTickerRepo.save(symbolTicker);
        }

        return opt.map(SymbolTicker::getTicker).orElse(null);
    }

    /*
    * 다중 파일 업로드시 사용되고 있음.
    * */
    @Transactional
    public ProcessResult saveTradeHistoryFromList(List<TradeHistoryJsonRecord> dtoList, Long userId, BrokerType brokerType) {
        try {
            if (dtoList == null || dtoList.isEmpty()) {
                return ProcessResult.builder().success(true).message("OK: empty").build();
            }
            User user = uRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            TradeHistoryDomainService.SaveResult result = tradeHistoryDomainService.saveTrades(
                    dtoList,
                    userId,
                    brokerType,
                    user,
                    dtoList.size()
            );

            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("OK: %d/%d : 중복되는 데이터는 건너뜀", result.savedCount(), result.totalCount()))
                    .processedCount(result.savedCount())
                    .totalCount(result.totalCount())
                    .build();
        } catch (Exception e) {
            return ProcessResult.builder().success(false).message("FAIL: " + e.getMessage()).build();
        }
    }

    /*
    * 단건 파일 업로드시 사용되고 있음.
    * */
    public ProcessResult saveTradeHistoryFromNdjsonStream(InputStream inputStream, Long userId, BrokerType brokerType) {
        try {
            TradeHistoryNdjsonAdapter.ParseResult parseResult = ndjsonAdapter.parse(inputStream);
            if (parseResult.empty()) {
                return ProcessResult.builder()
                        .success(false)
                        .message(parseResult.message())
                        .processedCount(0)
                        .totalCount(0)
                        .build();
            }

            User user = uRepo.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));

            TradeHistoryDomainService.SaveResult result = tradeHistoryDomainService.saveTrades(
                    parseResult.records(),
                    userId,
                    brokerType,
                    user,
                    parseResult.totalCount()
            );

            return ProcessResult.builder()
                    .success(true)
                    .message(String.format("%s: 총 %d건 중 %d건 저장 완료",
                            parseResult.message(), result.totalCount(), result.savedCount()))
                    .processedCount(result.savedCount())
                    .totalCount(result.totalCount())
                    .build();

        } catch (Exception e) {
            log.error("NDJSON 데이터 저장 실패: ", e);
            return ProcessResult.builder()
                    .success(false)
                    .message("데이터 저장 실패: " + e.getMessage())
                    .processedCount(0)
                    .totalCount(0)
                    .build();
        }
    }

    public List<TradeHistory> getHistoryWithinConditions(TradeSearchCondition condition) {
        try {
            if (condition.getPage() != null && condition.getSize() != null) {
                int p = Math.max(0, condition.getPage());
                int s = Math.max(1, condition.getSize());
                Sort sort = Sort.by(Sort.Order.desc("tradeDate"), Sort.Order.desc("trHistId"));
                Pageable pageable = PageRequest.of(p, s, sort);
                Page<TradeHistory> pg = repository.findAll(TradeHistorySpecification.withCondition(condition), pageable);
                return pg.getContent();
            } else {
                return repository.findAll(TradeHistorySpecification.withCondition(condition));
            }
        } catch(Exception e){
            log.error("거래내역 조회 실패: ", e);
            throw e;
        }

    }

//    public Optional<TradeHistory> getHistory(TradeSearchCondition condition) {
//        return repository.findOne(TradeHistorySpecification.withCondition(condition));
//    }
}
