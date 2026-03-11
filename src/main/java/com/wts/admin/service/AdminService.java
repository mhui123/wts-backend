package com.wts.admin.service;

import com.wts.admin.mapper.AdminAssembler;
import com.wts.api.dto.ProcessResult;
import com.wts.api.dto.StockDistributionDto;
import com.wts.api.service.PythonServerService;
import com.wts.kiwoom.dto.StockDto;
import com.wts.kiwoom.service.KiwoomApiService;
import com.wts.summary.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    private final DashboardService dashboardService;
    private final PythonServerService pythonServerService;
    private final KiwoomApiService kiwoomService;

    public ProcessResult collectDividendInfo(List<String> symbols) {
        try {
            ProcessResult result = pythonServerService.executeGetTask("/wpy/stock/dividendInfo", Map.of("symbols", symbols));

            if (!result.isSuccess() || result.getData() == null) {
                return result;
            }

            List<StockDistributionDto> distributions = parseDistributions(result.getData());
            return dashboardService.saveStockDistribution(distributions);

        } catch (Exception e) {
            log.error("배당 정보 수집 중 오류: ", e);
            return ProcessResult.builder().success(false).message("배당 정보 수집 중 오류가 발생했습니다.").build();
        }
    }

    /**
     * Python 응답 구조 변환
     * 입력: List<Map<ticker, List<Map<field, value>>>>
     * 출력: List<StockDistributionDto> (전체 종목의 배당 이력을 flat하게)
     */
    @SuppressWarnings("unchecked")
    private List<StockDistributionDto> parseDistributions(Object rawData) {
        List<StockDistributionDto> result = new ArrayList<>();

        if (!(rawData instanceof List<?> outerList)) {
            log.warn("배당 정보 파싱 실패: 예상 타입 List, 실제 타입 {}", rawData.getClass().getName());
            return result;
        }

        for (Object tickerEntry : outerList) {
            if (!(tickerEntry instanceof Map<?, ?> tickerMap)) continue;

            // { "385520.KS": [ {...}, {...} ] } 구조
            for (Map.Entry<?, ?> entry : tickerMap.entrySet()) {
                String ticker = String.valueOf(entry.getKey());

                if (!(entry.getValue() instanceof List<?> historyList)) {
                    log.warn("ticker={} 배당 이력이 List가 아닙니다. 건너뜁니다.", ticker);
                    continue;
                }

                for (Object historyItem : historyList) {
                    if (!(historyItem instanceof Map<?, ?> fields)) continue;
                    try {
                        StockDistributionDto dto = AdminAssembler.toDistributionDto(ticker, (Map<String, Object>) fields);
                        result.add(dto);
                    } catch (Exception e) {
                        log.warn("ticker={} 배당 항목 변환 실패, 건너뜁니다. fields={}, error={}", ticker, fields, e.getMessage());
                    }
                }
            }
        }

        return result;
    }


    private static final int CHUNK_SIZE = 500;

    //종목코드 최신화
    public ProcessResult syncStockCodes() {
        try {
            ProcessResult result = pythonServerService.executeGetTask("/wpy/stock/tickers", Map.of());

            if (!result.isSuccess() || result.getData() == null) {
                return result;
            }

            // 전체 리스트를 메모리에 올리지 않고 청크 단위로 파싱 → 저장
            List<StockDto> buffer = new ArrayList<>(CHUNK_SIZE);
            int[] totalStat = {0, 0, 0}; // [insert, update, skip]

            if (!(result.getData() instanceof List<?> rawList)) {
                log.warn("종목 정보 파싱 실패: 예상 타입 List, 실제 타입 {}", result.getData().getClass().getName());
                return ProcessResult.builder().success(false).message("응답 데이터 형식 오류").build();
            }

            for (Object entry : rawList) {
                StockDto dto = parseStockEntry(entry);
                if (dto == null) {
                    totalStat[2]++;
                    continue;
                }
                buffer.add(dto);

                if (buffer.size() >= CHUNK_SIZE) {
                    int[] stat = kiwoomService.saveStockCodeInfo(new ArrayList<>(buffer));
                    totalStat[0] += stat[0];
                    totalStat[1] += stat[1];
                    totalStat[2] += stat[2];
                    buffer.clear();
                    log.debug("종목 청크 처리 완료 - 누적 insert={}, update={}, skip={}", totalStat[0], totalStat[1], totalStat[2]);
                }
            }

            // 마지막 잔여 청크 처리
            if (!buffer.isEmpty()) {
                int[] stat = kiwoomService.saveStockCodeInfo(buffer);
                totalStat[0] += stat[0];
                totalStat[1] += stat[1];
                totalStat[2] += stat[2];
            }

            log.info("종목코드 최신화 완료 - insert={}, update={}, skip={}", totalStat[0], totalStat[1], totalStat[2]);
            return ProcessResult.builder()
                    .success(true)
                    .message("종목 정보 저장 완료. (신규: %d, 업데이트: %d, 건너뜀: %d)".formatted(totalStat[0], totalStat[1], totalStat[2]))
                    .build();

        } catch (Exception e) {
            log.error("종목코드 최신화 중 오류: ", e);
            return ProcessResult.builder().success(false).message("종목코드 최신화 중 오류가 발생했습니다.").build();
        }
    }

    /** 개별 항목 Map → StockDto 변환, 실패 시 null 반환 */
    private StockDto parseStockEntry(Object entry) {
        if (!(entry instanceof Map<?, ?> stockMap)) return null;
        try {
            String symbol = String.valueOf(stockMap.get("symbol"));
            String name   = String.valueOf(stockMap.get("name"));
            String market = String.valueOf(stockMap.get("market"));
            if (symbol.isBlank() || symbol.equals("null")) return null;
            return StockDto.builder().stockCd(symbol).stockNm(name).market(market).build();
        } catch (Exception e) {
            log.warn("종목 항목 변환 실패, 건너뜁니다. entry={}, error={}", entry, e.getMessage());
            return null;
        }
    }
}
